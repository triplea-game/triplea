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

import games.strategy.engine.data.*;
import games.strategy.engine.history.*;
import games.strategy.net.*;
import games.strategy.net.IRemote;

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
     * @see games.strategy.net.IRemoteMessenger
     */
    public IRemote getRemote(PlayerID id);

    /**
     * Player is initialized to the player specified in the xml data.
     */
    public void setPlayerID(PlayerID aPlayer);

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
     * Delegates should not use random data that comes from any other source.
     * 
     * @param annotation -
     *            a string used to describe the dice roll
     * @see games.strategy.engine.random.IRandomSource
     */
    public int getRandom(int max, String annotation);

    public int[] getRandom(int max, int count, String annotation);

    /**
     *  
     */
    public DelegateHistoryWriter getHistoryWriter();
    
    public IChannelSubscribor getDisplayChannelBroadcaster();

}