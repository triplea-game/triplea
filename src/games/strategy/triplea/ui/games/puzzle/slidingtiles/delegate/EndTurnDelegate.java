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

package games.puzzle.slidingtiles.delegate;

import java.util.concurrent.CountDownLatch;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.puzzle.slidingtiles.attachments.Tile;
import games.puzzle.slidingtiles.ui.display.INPuzzleDisplay;

/**
 * Responsible for checking for a winner in a game of n-puzzle.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2008-01-09 08:52:33 -0600 (Wed, 09 Jan 2008) $
 */
public class EndTurnDelegate extends BaseDelegate
{
    private CountDownLatch m_waiting;
    
    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge bridge, GameData gameData)
    {   
        super.start(bridge, gameData);
        
        if (gameOver(gameData.getMap()))
        {
            signalGameOver("Board solved!");
            
            try
            {
                m_waiting = new CountDownLatch(1);
                m_waiting.await();
            } catch (InterruptedException e)
            {
            }
        }

    }

    public boolean gameOver(GameMap map) 
    {
        int width = map.getXDimension();
        int height = map.getYDimension();
        
        int previous = -1 * Integer.MAX_VALUE;
        
        for(int y=0; y<height; y++)  
        {
            for (int x=0; x<width; x++)
            {
                Territory t = map.getTerritoryFromCoordinates(x, y);
                if (t!=null)
                {
                    Tile tile = (Tile) t.getAttachment("tile");
                    if (tile!=null)
                    {
                        int current = tile.getValue();
                        
                        if (current > previous)
                            previous = current;
                        else
                            return false;
                        
                    }
                    else
                        return false;
                } 
                else
                {
                    return false;
                }
                
            }
        }
        
        return true;
        
    }
    
    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {
        if (m_waiting==null)
            return;
        else
            while (m_waiting.getCount() > 0)
                m_waiting.countDown();
    }
    
    /**
     * Notify all players that the game is over.
     * 
     * @param status the "game over" text to be displayed to each user.
     */
    private void signalGameOver(String status)
    {
        // If the game is over, we need to be able to alert all UIs to that fact.
        //    The display object can send a message to all UIs.
        INPuzzleDisplay display = (INPuzzleDisplay) m_bridge.getDisplayChannelBroadcaster();
        display.setStatus(status);
        display.setGameOver();
    }
    
    
    /**
     * If this class implements an interface which inherits from IRemote, returns the class of that interface.
     * Otherwise, returns null.
     */
    public Class<? extends IRemote> getRemoteType()
    {
        // This class does not implement the IRemote interface, so return null.
        return null;
    }
}
