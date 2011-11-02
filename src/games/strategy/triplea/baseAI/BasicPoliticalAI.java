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
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;

import java.util.ArrayList;
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
	
	public static List<PoliticalActionAttachment> getPoliticalActionsTowardsWar(final PlayerID id)
	{
		final List<PoliticalActionAttachment> acceptableActions = new ArrayList<PoliticalActionAttachment>();
		for (final PoliticalActionAttachment nextAction : PoliticalActionAttachment.getValidActions(id))
		{
			if (wantToPerFormActionTowardsWar(nextAction, id))
			{
				acceptableActions.add(nextAction);
			}
		}
		return acceptableActions;
	}
	
	public static List<PoliticalActionAttachment> getPoliticalActionsOther(final PlayerID id)
	{
		final List<PoliticalActionAttachment> warActions = getPoliticalActionsTowardsWar(id);
		final List<PoliticalActionAttachment> acceptableActions = new ArrayList<PoliticalActionAttachment>();
		for (final PoliticalActionAttachment nextAction : PoliticalActionAttachment.getValidActions(id))
		{
			if (warActions.contains(nextAction))
				continue;
			if (awayFromAlly(nextAction, id) && Math.random() < .7)
				continue;
			if (isFree(nextAction))
				acceptableActions.add(nextAction);
			else if (acceptableActions.isEmpty())
			{
				if (Math.random() < .8 && isAcceptableCost(nextAction, id))
					acceptableActions.add(nextAction);
			}
			else if (!acceptableActions.isEmpty())
			{
				if (Math.random() < .4 && isAcceptableCost(nextAction, id))
					acceptableActions.add(nextAction);
			}
		}
		return acceptableActions;
	}
	
	private static boolean wantToPerFormActionTowardsWar(final PoliticalActionAttachment nextAction, final PlayerID id)
	{
		return isFree(nextAction) && goesTowardsWar(nextAction, id);
	}
	
	// this code has a rare risk of circular loop actions.. depending on the map
	// designer
	// only switches from a Neutral to an War state... won't go through
	// in-between neutral states
	// TODO have another look at this part.
	private static boolean goesTowardsWar(final PoliticalActionAttachment nextAction, final PlayerID p0)
	{
		final GameData data = p0.getData();
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
	
	private static boolean awayFromAlly(final PoliticalActionAttachment nextAction, final PlayerID p0)
	{
		final GameData data = p0.getData();
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
	
	private static boolean isAcceptableCost(final PoliticalActionAttachment nextAction, final PlayerID player)
	{
		// if we have 21 or more PUs and the cost of the action is l0% or less of our total money, then it is an acceptable price.
		final GameData data = player.getData();
		final float production = AbstractEndTurnDelegate.getProduction(data.getMap().getTerritoriesOwnedBy(player), data);
		return production >= 21 && (nextAction.getCostPU()) <= ((production / 10));
	}
	
}
