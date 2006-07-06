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

package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.lobby.server.*;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.message.*;
import games.strategy.net.*;

import java.util.*;

/**
 * 
 * Watches a game in progress, and updates the Lobby with the state of the game.<p>
 * 
 * This class opens its own connection to the lobby, and its own messenger.<p>
 * 
 * @author sgb
 *
 */
public class InGameLobbyWatcher
{
    
    public static final String LOBBY_WATCHER_NAME = "in_game_lobby_watcher";
    
    private final GUID m_gameID = new GUID();
    private final IMessenger m_messenger;
    private final IRemoteMessenger m_remoteMessenger;

    private final Object m_mutex = new Object();
    
    /**
     * Reads SystemProperties to see if we should connect to a lobby server<p>
     * 
     * After creation, those properties are cleared, since we should watch the first start game.<p>
     * 
     * @return null if no watcher should be created
     */
    public static InGameLobbyWatcher newInGameLobbyWatcher()
    {
        String host = System.getProperties().getProperty(GameRunner2.LOBBY_HOST);
        String port = System.getProperties().getProperty(GameRunner2.LOBBY_PORT);
        
        if(host == null || port == null)
        {
            return null;
        }
        
        //clear the properties
        System.getProperties().remove(GameRunner2.LOBBY_HOST);
        System.getProperties().remove(GameRunner2.LOBBY_PORT);
        
        IConnectionLogin login = new IConnectionLogin()
        {
        
            public void notifyFailedLogin(String message)
            {}
        
            public Map<String, String> getProperties(Map<String, String> challengProperties)
            {
                Map<String,String> rVal = new HashMap<String,String>();
                rVal.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
                rVal.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
                return rVal;
            }
        };
        
        try
        {
            System.out.println("host:" + host + " port:" + port);
            ClientMessenger messenger = new ClientMessenger(host, Integer.parseInt(port),LOBBY_WATCHER_NAME, login);
            UnifiedMessenger um = new UnifiedMessenger(messenger);
            RemoteMessenger rm = new RemoteMessenger(um);
            return new InGameLobbyWatcher(messenger, rm);
        }  catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    
    
    public InGameLobbyWatcher(final IMessenger messenger, final IRemoteMessenger remoteMessenger)
    {
        m_messenger = messenger;
        m_remoteMessenger = remoteMessenger;
        
        ILobbyGameController controller = (ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
        synchronized(m_mutex)
        {
            GameDescription description = new GameDescription(m_messenger.getLocalNode(), 1, new Date(), "", 0, GameStatus.WAITING_FOR_PLAYERS, -1);
            controller.postGame(m_gameID, description);
        }
    }
    
    
    public void shutDown()
    {
        m_messenger.shutDown();
    }
    
    

}
