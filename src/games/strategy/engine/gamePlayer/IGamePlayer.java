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
 * GamePlayer.java
 *
 * Created on October 27, 2001, 5:15 PM
 */

package games.strategy.engine.gamePlayer;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.*;

/**
 * 
 * A player of the game.
 * <p>
 * Game players communicate to the game through a PlayerBridge.
 * 
 * @author Sean Bridges
 * @version 1.0
 *  
 */
public interface IGamePlayer extends IDestination
{
    public void initialize(IPlayerBridge bridge, PlayerID id);

    public Message sendMessage(Message message);

    public String getName();

    public PlayerID getID();

    public void start(String stepName);

    /**
     * Get the type of the GamePlayer.
     * <p>
     * The type must extend IRemote, and is to be used by an IRemoteManager to
     * allow a player to be contacted remotately
     * 
     * @see games.strategy.net.IRemoteMessenger
     */
    public Class getRemotePlayerType();

}