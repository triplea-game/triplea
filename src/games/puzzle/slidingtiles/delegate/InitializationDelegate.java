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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import games.puzzle.slidingtiles.attachments.Tile;
import games.puzzle.slidingtiles.ui.display.INPuzzleDisplay;
import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;

/**
 * Responsible for initializing an N-Puzzle game.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class InitializationDelegate extends BaseDelegate
{
    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge bridge, GameData gameData)
    {   
        super.start(bridge, gameData);

        GameMap map = gameData.getMap();
        
        int width = map.getXDimension();
        int height = map.getYDimension();
        
        Territory[][] board = new Territory[width][height];
        
        INPuzzleDisplay display = (INPuzzleDisplay) m_bridge.getDisplayChannelBroadcaster();
        display.setStatus("Shuffling tiles...");
        
        m_bridge.getHistoryWriter().startEvent("Initializing board");
        
        CompositeChange initializingBoard = new CompositeChange();
        
        for (int x=0; x<width; x++) {
            for(int y=0; y<height; y++) {
                board[x][y] = map.getTerritoryFromCoordinates(x, y);
                Tile tile = new Tile(x + y*width);
                //System.out.println("board["+x+"]["+y+"]=="+(x + y*width));
                Change change = ChangeFactory.addAttachmentChange(tile, board[x][y], "tile");
                initializingBoard.add(change);
            }
        }

        m_bridge.addChange(initializingBoard);

        //INPuzzleDisplay display = (INPuzzleDisplay) m_bridge.getDisplayChannelBroadcaster();
        display.initializeBoard();
        display.performPlay();  
        
        
        m_bridge.getHistoryWriter().startEvent("Randomizing board");
        //CompositeChange randomizingBoard = new CompositeChange();
        
        
        Territory blank = board[0][0];
        Territory dontChooseNextTime = null;
        Territory swap = null;
        
       //System.out.println("Random stuff!");
        GameProperties properties = gameData.getProperties();
        int numberOfShuffles = Integer.valueOf((String) properties.get("Difficulty Level"));
        //int numberOfShuffles = 0;
        // Randomly shuffle the tiles on the board,
        //   but don't move a tile back to where it just was.
        Random random = new Random();
        for (int i=0; i<numberOfShuffles; i++)
        {   
            while (swap==null || swap.equals(dontChooseNextTime)) 
            {   
                List<Territory> neighbors = new ArrayList<Territory>(map.getNeighbors(blank));
                swap = neighbors.get(random.nextInt(neighbors.size()));
            }
            
            try{ Thread.sleep(75);} 
            catch (InterruptedException e){}
            
            PlayDelegate.swap(m_bridge, swap, blank);
            //randomizingBoard.add(change);
            
            dontChooseNextTime = blank;
            blank = swap;
            swap = null;
            
        }
        
        display.setStatus(" ");
        
        //m_bridge.addChange(randomizingBoard);
        //display.performPlay();  
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
