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

package games.puzzle.tictactoe.delegate;

import java.util.ArrayList;
import java.util.Collection;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.puzzle.tictactoe.delegate.remote.IPlayDelegate;
import games.puzzle.tictactoe.ui.display.ITicTacToeDisplay;

/**
 * Responsible for performing a move in a game of Tic Tac Toe.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2008-02-02 12:09:22 -0600 (Sat, 02 Feb 2008) $
 */
public class PlayDelegate extends BaseDelegate implements IPlayDelegate
{
    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge bridge, GameData gameData)
    {
        super.start(bridge, gameData);
        
        
        ITicTacToeDisplay display = (ITicTacToeDisplay) bridge.getDisplayChannelBroadcaster();        
        display.setStatus(m_player.getName() + "'s turn");
    }

    /**
     * Attempt to play.
     *  
     * @param play <code>Territory</code> where the play should occur
     */
    public String play(Territory play)
    {   
        String error = isValidPlay(play);
        if (error != null)
            return error;
        
        performPlay(play,m_player);
        
        return null;
    }

    
    /**
     * Check to see if a play is valid.
     * 
     * @param play <code>Territory</code> where the play should occur
     */
    private String isValidPlay(Territory territory)
    {
        if (territory.getOwner().equals(PlayerID.NULL_PLAYERID))
            return null;
        else
            return "Square is not empty";
    }
   
    
    /**
     * Perform a play.
     * 
     * @param play <code>Territory</code> where the play should occur
     */
    private void performPlay(Territory at, PlayerID player)
    {          
        Collection<Unit> units = new ArrayList<Unit>(1);
        units.add(m_data.getUnitTypeList().getUnitType("ticmark").create(player));

        String transcriptText = player.getName() + " played in " + at.getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        m_bridge.getHistoryWriter().setRenderingData(units);

        Change place = ChangeFactory.addUnits(at, units);
        Change owner = ChangeFactory.changeOwner(at, player);

        CompositeChange change = new CompositeChange();
        change.add(place);
        change.add(owner);

        m_bridge.addChange(change);

        ITicTacToeDisplay display = (ITicTacToeDisplay) m_bridge.getDisplayChannelBroadcaster();
        display.performPlay(at);
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
