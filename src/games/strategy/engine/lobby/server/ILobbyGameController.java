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

import games.strategy.engine.message.IRemote;
import games.strategy.net.GUID;

public interface ILobbyGameController extends IRemote
{
    public static final String GAME_CONTROLLER_REMOTE = "games.strategy.engine.lobby.server.IGameController.GAME_CONTROLLER_REMOTE";

    public void postGame(GUID gameID, GameDescription description);
    
    public void updateGame(GUID gameID, GameDescription description);
    
    public Map<GUID, GameDescription> listGames();
    
}
