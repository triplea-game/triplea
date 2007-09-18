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

/*
 * DelegateBridge.java
 *
 * Created on October 13, 2001, 4:35 PM
 */

package games.strategy.engine.delegate;

import java.util.Properties;

import games.strategy.engine.data.*;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.*;

/**
 * 
 * 
 * 
 * A class that communicates with the Delegate. DelegateBridge co-ordinates
 * comunication between the Delegate and both the players and the game data.
 * 
 * The reason for communicating through a DelegateBridge is to achieve network
 * transparancy.
 * 
 * The delegateBridge allows the Delegate to talk to the player in a safe
 * manner.
 * 
 * @author Sean Bridges
 */
public interface IDelegateBridge
{
  
    /**
     * equivalent to getRemote(getPlayerID())
     * 
     * @return remote for the current player.
     */
    public IRemote getRemote();

    /**
     * Get a remote reference to the given player.
     * 
     * @see games.strategy.engine.message.IRemoteMessenger
     */
    public IRemote getRemote(PlayerID id);

    public PlayerID getPlayerID();

    /**
     * Returns the current step name
     */
    public String getStepName();

    /**
     * Add a change to game data. Use this rather than changing gameData
     * directly since this method allows us to send the changes to other
     * machines.
     * 
     * @param aChange
     */
    public void addChange(Change aChange);

    /**
     * equivalent to getRandom(max,1,annotation)[0] 
     */
    public int getRandom(int max, String annotation);

    /**
     * 
     * Return a random value to be used by the delegate.<p>
     * 
     * Delegates should not use random data that comes from any other source.<p>
     * 
     * @param annotation -
     *            a string used to describe the random event.<p>
     */
    public int[] getRandom(int max, int count, String annotation);

    /**
     * return the delegate history writer for this game.<p>
     * 
     * The delegate history writer allows writing to the game history.<p>
     */
    public IDelegateHistoryWriter getHistoryWriter();
    
    /**
     * Return an object that implements the IDisplay interface for the game.<p>
     * 
     * Methods called on this returned object will be invoked on all displays in the game, 
     * including those on remote machines<p>
     */
    public IChannelSubscribor getDisplayChannelBroadcaster();
    
    /**
     * 
     * @return the propertie for this step.<p>
     */
    public Properties getStepProperties();
    
    
    /**
     * After this step finishes executing, the next delegate will not be called.<p>
     * 
     * This methd allows the delegate to signal that the game is over, but does not force the ui
     * or the display to shutdown.<p>
     *
     */
    public void stopGameSequence();
    
    public void leaveDelegateExecution();
    
    public void enterDelegateExecution();
    

}
