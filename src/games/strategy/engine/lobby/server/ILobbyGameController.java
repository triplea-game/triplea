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

package games.strategy.engine.lobby.server;

import java.util.Map;

import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.IRemote;
import games.strategy.net.GUID;

public interface ILobbyGameController extends IRemote
{
    public static final RemoteName GAME_CONTROLLER_REMOTE = new RemoteName("games.strategy.engine.lobby.server.IGameController.GAME_CONTROLLER_REMOTE", ILobbyGameController.class);

    public void postGame(GUID gameID, GameDescription description);
    
    public void updateGame(GUID gameID, GameDescription description);
    
    public Map<GUID, GameDescription> listGames();
    
    /**
     * Test if the server can connect to the game at this address.  This is used to see if the client address is network accessible
     * (this will not be true if the client is behind a nat or firewall that is not properly configured)
     * <p>
     * 
     * This method may only be called by the node that is hosting this game.<p>
     */
    public boolean testGame(GUID gameID);
    
}
