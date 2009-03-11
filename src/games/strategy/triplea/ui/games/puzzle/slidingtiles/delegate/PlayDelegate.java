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

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.puzzle.slidingtiles.attachments.Tile;
import games.puzzle.slidingtiles.delegate.remote.IPlayDelegate;
import games.puzzle.slidingtiles.ui.display.INPuzzleDisplay;

/**
 * Responsible for performing a move in a game of n-puzzle.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2008-01-09 08:52:33 -0600 (Wed, 09 Jan 2008) $
 */
public class PlayDelegate extends BaseDelegate implements IPlayDelegate
{
    private GameMap map;
    
    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge bridge, GameData gameData)
    {
        super.start(bridge, gameData);
        map = gameData.getMap();
    }

    /**
     * Attempt to play.
     *  
     * @param play <code>Territory</code> where the play should occur
     */
    public String play(Territory from, Territory to)
    {   
        if (from.equals(to))
        {
            Tile fromTile = (Tile) from.getAttachment("tile");
            if (fromTile!=null && fromTile.getValue()!=0)
            {
                Territory blank = getBlankNeighbor(map, from);
                
                if (blank==null)
                    return "Invalid move";
                else
                    to=blank;
            }  
        } 
        else 
        {
            String error = isValidPlay(from, to);
            if (error != null)
                return error;  
        }

        performPlay(from, to ,m_player);
        
        return null;
    }

    public void signalStatus(String status)
    {
        INPuzzleDisplay display = (INPuzzleDisplay) m_bridge.getDisplayChannelBroadcaster();
        display.setStatus(status);
    }
    
    public static Territory getBlankNeighbor(GameMap map, Territory t)
    {
        for (Territory neighbor : map.getNeighbors(t))
        {
            Tile neighborTile = (Tile) neighbor.getAttachment("tile");
            if (neighborTile!=null && neighborTile.getValue()==0)
            {
                return neighbor;
            }
        }
        
        return null;
    }
    
    /**
     * Check to see if a play is valid.
     * 
     * @param play <code>Territory</code> where the play should occur
     */
    private String isValidPlay(Territory from, Territory to)
    {
        int startValue = ((Tile) from.getAttachment("tile")).getValue();
        int destValue = ((Tile) to.getAttachment("tile")).getValue();
        
        if (startValue!=0 && destValue==0)
            return null;
        else
            return "Move does not swap a tile with the blank square";
       
        /*
        if (territory.getOwner().equals(PlayerID.NULL_PLAYERID))
            return null;
        else
            return "Square is not empty";
            */
        //return "Playing not yet implemented.";
    }
   
    
    /**
     * Perform a play.
     * 
     * @param play <code>Territory</code> where the play should occur
     */
    private void performPlay(Territory from, Territory to, PlayerID player)
    {          

        String transcriptText = player.getName() + " moved tile from " + from.getName() + " to " + to.getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        
        swap(m_bridge, from, to);

    }
    
    static void swap(IDelegateBridge bridge, Territory from, Territory to)
    {
        Tile fromAttachment = (Tile) from.getAttachment("tile");
        Tile toAttachment = (Tile) to.getAttachment("tile");
        
        int fromValue = fromAttachment.getValue();
        int toValue = toAttachment.getValue();

        Change fromChange = ChangeFactory.attachmentPropertyChange(fromAttachment, Integer.toString(toValue), "value");
        Change toChange = ChangeFactory.attachmentPropertyChange(toAttachment, Integer.toString(fromValue), "value");
       
        CompositeChange change = new CompositeChange();
        change.add(fromChange);
        change.add(toChange);
      
        bridge.addChange(change);

        INPuzzleDisplay display = (INPuzzleDisplay) bridge.getDisplayChannelBroadcaster();
        display.performPlay();  
        
        //return change;
    }
    
    /**
     * If this class implements an interface which inherits from IRemote, returns the class of that interface.
     * Otherwise, returns null.
     */
    public Class<? extends IRemote> getRemoteType()
    {
        // This class implements IPlayDelegate, which inherits from IRemote.
        return IPlayDelegate.class;
    }
}
