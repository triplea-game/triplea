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
package games.puzzle.slidingtiles.player;

import games.puzzle.slidingtiles.attachments.Tile;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.player.GridAbstractAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * AI agent for N-Puzzle.
 * 
 * Plays by attempting to play on a random square on the board.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
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
		final GameMap map = getGameData().getMap();
		final Collection<Territory> territories = map.getTerritories();
		// Get the play delegate
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemote();
		// Find the blank tile
		Territory blank = null;
		for (final Territory t : territories)
		{
			final Tile tile = (Tile) t.getAttachment("tile");
			if (tile != null)
			{
				final int value = tile.getValue();
				if (value == 0)
				{
					blank = t;
					break;
				}
			}
		}
		if (blank == null)
			throw new RuntimeException("No blank tile");
		final Random random = new Random();
		final List<Territory> neighbors = new ArrayList<Territory>(map.getNeighbors(blank));
		final Territory swap = neighbors.get(random.nextInt(neighbors.size()));
		playDel.play(swap, blank);
	}
}
