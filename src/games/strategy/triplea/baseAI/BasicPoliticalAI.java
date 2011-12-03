/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.baseAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Basic utility methods to handle basic AI stuff for Politics this AI always
 * tries to get from Neutral to War with state if it is free with everyone this
 * AI will not go through a different Neutral state to reach a War state. (ie go
 * from NAP to Peace to War)
 * 
 * @author Edwin van der Wal
 * 
 */
public class BasicPoliticalAI
{
	public static List<PoliticalActionAttachment> getPoliticalActionsTowardsWar(final PlayerID id, final HashMap<ICondition, Boolean> testedConditions, final GameData data)
	{
		final List<PoliticalActionAttachment> acceptableActions = new ArrayList<PoliticalActionAttachment>();
		for (final PoliticalActionAttachment nextAction : PoliticalActionAttachment.getValidActions(id, testedConditions, data))
		{
			if (wantToPerFormActionTowardsWar(nextAction, id, data))
			{
				acceptableActions.add(nextAction);
			}
		}
		return acceptableActions;
	}
	
	public static List<PoliticalActionAttachment> getPoliticalActionsOther(final PlayerID id, final HashMap<ICondition, Boolean> testedConditions, final GameData data)
	{
		final List<PoliticalActionAttachment> warActions = getPoliticalActionsTowardsWar(id, testedConditions, data);
		final List<PoliticalActionAttachment> acceptableActions = new ArrayList<PoliticalActionAttachment>();
		final Collection<PoliticalActionAttachment> validActions = PoliticalActionAttachment.getValidActions(id, testedConditions, data);
		validActions.removeAll(warActions);
		for (final PoliticalActionAttachment nextAction : validActions)
		{
			if (warActions.contains(nextAction))
				continue;
			if (goesTowardsWar(nextAction, id, data) && Math.random() < .5)
				continue;
			if (awayFromAlly(nextAction, id, data) && Math.random() < .9)
				continue;
			if (isFree(nextAction))
				acceptableActions.add(nextAction);
			else if (Match.countMatches(validActions, Matches.PoliticalActionHasCostBetween(0, 0)) > 1)
			{
				if (Math.random() < .9 && isAcceptableCost(nextAction, id, data))
					acceptableActions.add(nextAction);
			}
			else
			{
				if (Math.random() < .4 && isAcceptableCost(nextAction, id, data))
					acceptableActions.add(nextAction);
			}
		}
		return acceptableActions;
	}
	
	private static boolean wantToPerFormActionTowardsWar(final PoliticalActionAttachment nextAction, final PlayerID id, final GameData data)
	{
		return isFree(nextAction) && goesTowardsWar(nextAction, id, data);
	}
	
	// this code has a rare risk of circular loop actions.. depending on the map
	// designer
	// only switches from a Neutral to an War state... won't go through
	// in-between neutral states
	// TODO have another look at this part.
	private static boolean goesTowardsWar(final PoliticalActionAttachment nextAction, final PlayerID p0, final GameData data)
	{
		for (final String relationshipChangeString : nextAction.getRelationshipChange())
		{
			final String[] relationshipChange = relationshipChangeString.split(":");
			final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
			final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
			// only continue if p1 or p2 is the AI
			if (p0.equals(p1) || p0.equals(p2))
			{
				final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
				final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
				if (currentType.getRelationshipTypeAttachment().isNeutral() && newType.getRelationshipTypeAttachment().isWar())
					return true;
			}
		}
		return false;
	}
	
	private static boolean awayFromAlly(final PoliticalActionAttachment nextAction, final PlayerID p0, final GameData data)
	{
		for (final String relationshipChangeString : nextAction.getRelationshipChange())
		{
			final String[] relationshipChange = relationshipChangeString.split(":");
			final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
			final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
			// only continue if p1 or p2 is the AI
			if (p0.equals(p1) || p0.equals(p2))
			{
				final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
				final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
				if (currentType.getRelationshipTypeAttachment().isAllied() && (newType.getRelationshipTypeAttachment().isNeutral() || newType.getRelationshipTypeAttachment().isWar()))
					return true;
			}
		}
		return false;
	}
	
	private static boolean isFree(final PoliticalActionAttachment nextAction)
	{
		return nextAction.getCostPU() <= 0;
	}
	
	private static boolean isAcceptableCost(final PoliticalActionAttachment nextAction, final PlayerID player, final GameData data)
	{
		// if we have 21 or more PUs and the cost of the action is l0% or less of our total money, then it is an acceptable price.
		final float production = AbstractEndTurnDelegate.getProduction(data.getMap().getTerritoriesOwnedBy(player), data);
		return production >= 21 && (nextAction.getCostPU()) <= ((production / 10));
	}
}
