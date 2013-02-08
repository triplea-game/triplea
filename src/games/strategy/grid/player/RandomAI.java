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
package games.strategy.grid.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.grid.checkers.delegate.PlayDelegate;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * AI agent for most any Grid Games.
 * 
 * Plays by attempting to move a random piece to a random square on the board.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2011-11-22 18:21:37 +0800 (Tue, 22 Nov 2011) $
 */
public class RandomAI extends GridAbstractAI
{
	public RandomAI(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void play()
	{
		// Unless the triplea.ai.pause system property is set to false,
		// pause for 0.8 seconds to give the impression of thinking
		pause();
		// Get the collection of territories from the map
		final GameData data = getGameData();
		final PlayerID me = getPlayerID();
		Territory tWithUnits = null;
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.getUnits().getUnitCount() > 0)
			{
				tWithUnits = t;
				break;
			}
		}
		final Collection<Territory> allTerritories = PlayDelegate.getAllTerritoriesOnMapWhichCanHaveUnits(tWithUnits, data);
		final Collection<Territory> myTerritories = Match.getMatches(allTerritories, PlayDelegate.TerritoryHasUnitsOwnedBy(me));
		final List<GridPlayData> validMoves = new ArrayList<GridPlayData>();
		for (final Territory t : myTerritories)
		{
			validMoves.addAll(PlayDelegate.getAllValidMovesFromHere(t, me, data));
		}
		Collections.shuffle(validMoves);
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemote();
		final Iterator<GridPlayData> iter = validMoves.iterator();
		String error;
		do
		{
			error = playDel.play(iter.next());
		} while (error != null && iter.hasNext());
	}
}
