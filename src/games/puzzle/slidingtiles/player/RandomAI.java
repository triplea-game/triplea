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

package games.puzzle.slidingtiles.player;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.puzzle.slidingtiles.attachments.Tile;
import games.puzzle.slidingtiles.delegate.remote.IPlayDelegate;

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
 * @version $LastChangedDate: 2007-06-27 19:20:21 -0500 (Wed, 27 Jun 2007) $
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
        GameMap map = getGameData().getMap();
        Collection<Territory> territories = map.getTerritories();
        
        // Get the play delegate
        IPlayDelegate playDel = (IPlayDelegate) this.getPlayerBridge().getRemote();
        
        // Find the blank tile
        Territory blank = null;
        for (Territory t : territories)
        {
            Tile tile = (Tile) t.getAttachment("tile");
            if (tile!=null)
            {
               int value = tile.getValue();
               if (value==0)
               {
                   blank = t;
                   break;
               }
               
            }
        }
        
        if (blank==null)
            throw new RuntimeException("No blank tile");
        
        Random random = new Random();
        List<Territory> neighbors = new ArrayList<Territory>(map.getNeighbors(blank));
        Territory swap = neighbors.get(random.nextInt(neighbors.size()));
        
        playDel.play(swap, blank);
        
    }

}
