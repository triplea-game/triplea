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

package games.strategy.engine.framework.startup.mc;

import java.util.Map;

import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;

/**
 * A callback remote.
 * 
 * Allows the server to add the player as an observer when the game is in progress.
 * 
 * @author sgb
 */
public interface IObserverWaitingToJoin extends IRemote
{
    /**
     * This method should not return until the client is ready to start the game.
     * This includes the display running, with all remote and channel listeners set up.
     */
    public void joinGame(byte[] gameData, Map<String, INode> players);

    /**
     * You could not join the game, usually this is due to an error.
     */
    public void cannotJoinGame(String reason);
   
}
