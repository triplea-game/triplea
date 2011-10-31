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

import java.util.ArrayList;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;

/**
 * Basic utility methods to handle basic AI stuff for Politics this AI always
 * tries to get from Neutral to War with state if it is free with everyone this
 * AI will not go through a different Neutral state to reach a War state. (ie go
 * from NAP to Peace to War)
 * 
 * @author Edwin van der Wal
 * 
 */
public class BasicPoliticalAI {

	public static List<PoliticalActionAttachment> getPoliticalAction(PlayerID id) {
		List<PoliticalActionAttachment> acceptableActions = new ArrayList<PoliticalActionAttachment>();
		for (PoliticalActionAttachment nextAction : PoliticalActionAttachment.getValidActions(id)) {
			if (wantToPerFormAction(nextAction, id)) {
				acceptableActions.add(nextAction);
			}
		}
		return acceptableActions;
	}

	private static boolean wantToPerFormAction(PoliticalActionAttachment nextAction, PlayerID id) {
		return isFree(nextAction) && goesTowardsWar(nextAction, id);
	}

	// this code has a rare risk of circular loop actions.. depending on the map
	// designer
	// only switches from a Neutral to an War state... won't go through
	// in-between neutral states
	// TODO have another look at this part.
	private static boolean goesTowardsWar(PoliticalActionAttachment nextAction, PlayerID p0) {
		GameData data = p0.getData();
		for (String relationshipChangeString : nextAction.getRelationshipChange()) {
			String[] relationshipChange = relationshipChangeString.split(":");
			PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
			PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
			// only continue if p1 or p2 is the AI
			if (p0.equals(p1) || p0.equals(p2)) {
				RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
				RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
				if (currentType.getRelationshipTypeAttachment().isNeutral() && newType.getRelationshipTypeAttachment().isWar())
					return true;
			}

		}
		return false;
	}

	private static boolean isFree(PoliticalActionAttachment nextAction) {
		return nextAction.getCostPU() <= 0;
	}

}
