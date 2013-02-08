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

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;

import java.util.Collection;
import java.util.Random;

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
		final Collection<Territory> territories = getGameData().getMap().getTerritories();
		final Territory[] territoryArray = territories.toArray(new Territory[territories.size()]);
		final Random generator = new Random();
		int trymeStart;
		int trymeEnd;
		String error;
		// Get the play delegate
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemote();
		final PlayerID me = getPlayerID();
		// Randomly select a territory and try playing there
		// If that play isn't legal, try again
		do
		{
			trymeStart = generator.nextInt(territoryArray.length);
			trymeEnd = generator.nextInt(territoryArray.length);
			final IGridPlayData play = new GridPlayData(territoryArray[trymeStart], territoryArray[trymeEnd], me);
			error = playDel.play(play);
		} while (error != null);
	}
}
