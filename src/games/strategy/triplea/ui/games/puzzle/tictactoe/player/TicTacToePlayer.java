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

import games.strategy.common.player.AbstractHumanPlayer;
import games.strategy.engine.data.Territory;
import games.puzzle.tictactoe.delegate.remote.IPlayDelegate;
import games.puzzle.tictactoe.ui.TicTacToeFrame;

/**
 * Represents a human player of Tic Tac Toe.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2008-02-02 12:09:22 -0600 (Sat, 02 Feb 2008) $
 */
public class TicTacToePlayer extends AbstractHumanPlayer<TicTacToeFrame> implements ITicTacToePlayer
{
	
    public TicTacToePlayer(String name)
    {
        super(name);
    }
    

    @Override
    public void start(String stepName)
    {
        if (stepName.endsWith("Play"))
            play();
        else
            throw new IllegalArgumentException("Unrecognized step stepName:" + stepName);
    }
    
    private void play() 
    {   
    	// Get the relevant delegate
        IPlayDelegate playDel = (IPlayDelegate) m_bridge.getRemote();
        Territory play = null;
        
        while (play == null)
        {   
        	play = m_ui.waitForPlay(m_id, m_bridge);

            if (play == null)
            {
            	// If play==null, the play was interrupted,
            	//    most likely by the player trying to leave the game.
            	//    So, we should not try asking the UI to get a new play.
            	return;
            }
            else
            {
            	// A play was returned from the user interface.
            	//    We need to have the relevant delegate process it
            	//    and see if there are any problems with the play.
            	String error = playDel.play(play);
            	
            	if(error != null)
            	{	
            		// If there is a problem with the play, notify the user...
            		m_ui.notifyError(error);
            		
            		// ... then have the user try again.
            		play = null;
            	}
            }

        }
    }

}
