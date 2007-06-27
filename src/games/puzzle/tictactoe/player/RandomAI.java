/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.puzzle.tictactoe.player;

import games.strategy.engine.data.Territory;
import games.puzzle.tictactoe.delegate.remote.IPlayDelegate;

import java.util.Collection;
import java.util.Random;

/**
 * AI agent for King's Table.
 * 
 * Plays by attempting to move a random piece to a random square on the board.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2007-06-19 13:39:15 -0500 (Tue, 19 Jun 2007) $
 */
public class RandomAI extends AbstractAI
{

    public RandomAI(String name)
    {
        super(name);
    }

    @Override
    protected void play()
    {
        // Unless the triplea.ai.pause system property is set to false,
        //    pause for 0.8 seconds to give the impression of thinking
        pause();
        
        // Get the collection of territories from the map
        Collection<Territory> territories = getGameData().getMap().getTerritories();
        Territory[] territoryArray = territories.toArray(new Territory[territories.size()]);
        
        Random generator = new Random();
        int trymeStart;
        int trymeEnd;
        String error;
        
        // Get the play delegate
        IPlayDelegate playDel = (IPlayDelegate) this.getPlayerBridge().getRemote();
        
        // Randomly select a territory and try playing there
        // If that play isn't legal, try again
        do
        {   trymeStart = generator.nextInt(territoryArray.length);
            //trymeEnd = generator.nextInt(territoryArray.length);
            error = playDel.play(territoryArray[trymeStart]);
        } while (error != null);
        
        

    }

}
