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
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
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
    
    //this is the messenger used by the game
    //it is different than the messenger we use to connect to 
    //the game lobby
    private final IServerMessenger m_gameMessenger;
    private boolean m_shutdown = false;
    
    private final GUID m_gameID = new GUID();
    
    private GameSelectorModel m_gameSelectorModel;
    private Observer m_gameSelectorModelObserver = new Observer()
    {
    
        public void update(Observable o, Object arg)
        {   
            gameSelectorModelUpdated();
        }
    };
    
    //we create this messenger, and use it to connect to the 
    //game lobby
    private final IMessenger m_messenger;
    private final IRemoteMessenger m_remoteMessenger;
    private GameDescription m_gameDescription;

    private final Object m_mutex = new Object();

    private IConnectionChangeListener m_connectionChangeListener;
    
    /**
     * Reads SystemProperties to see if we should connect to a lobby server<p>
     * 
     * After creation, those properties are cleared, since we should watch the first start game.<p>
     * 
     * @return null if no watcher should be created
     */
    public static InGameLobbyWatcher newInGameLobbyWatcher(IServerMessenger gameMessenger)
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
            return new InGameLobbyWatcher(messenger, rm, gameMessenger);
        }  catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    
    
    private void gameSelectorModelUpdated()
    {
        synchronized(m_mutex)
        {
            m_gameDescription.setGameName(m_gameSelectorModel.getGameName());
            m_gameDescription.setRound(m_gameSelectorModel.getGameRound());
            postUpdate();
        }
        
    }



    public InGameLobbyWatcher(final IMessenger messenger, final IRemoteMessenger remoteMessenger, final IServerMessenger serverMessenger)
    {
        m_messenger = messenger;
        m_remoteMessenger = remoteMessenger;
        
        m_gameMessenger = serverMessenger;
        
        m_gameDescription = new GameDescription(m_messenger.getLocalNode(), m_gameMessenger.getLocalNode().getPort(), new Date(), "???", 1, GameStatus.WAITING_FOR_PLAYERS, "-", m_gameMessenger.getLocalNode().getName(), System.getProperty(GameRunner2.LOBBY_GAME_COMMENTS));
        
        ILobbyGameController controller = (ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
        synchronized(m_mutex)
        {

            controller.postGame(m_gameID, (GameDescription) m_gameDescription.clone());
        }
        
        //if we loose our connection, then shutdown
        m_messenger.addErrorListener(new IMessengerErrorListener()
        {
        
            public void messengerInvalid(IMessenger messenger, Exception reason, List unsent)
            {
                shutDown();
            }
        
            public void connectionLost(INode node, Exception reason, List unsent)
            {}
        
        });
        
        
        m_connectionChangeListener = new IConnectionChangeListener()
                        {
                        
                            public void connectionRemoved(INode to)
                            {
                                updatePlayerCount();
                        
                            }
                        
                            public void connectionAdded(INode to)
                            {
                                updatePlayerCount();
                        
                            }
                        
                        };
        //when players join or leave the game
        //update the connection count
        m_gameMessenger.addConnectionChangeListener(m_connectionChangeListener);
        
    }
    
    public void setGameSelectorModel(GameSelectorModel model)
    {
        cleanUpGameModelListener();
        
        if(model != null)
        {
            m_gameSelectorModel = model;
            m_gameSelectorModel.addObserver(m_gameSelectorModelObserver);
            gameSelectorModelUpdated();
        }
    }



    private void cleanUpGameModelListener()
    {
        if(m_gameSelectorModel != null)
        {
            m_gameSelectorModel.deleteObserver(m_gameSelectorModelObserver);
        }
    }
    
    
    
    protected void updatePlayerCount()
    {
        synchronized(m_mutex)
        {
            m_gameDescription.setPlayerCount(m_gameMessenger.getNodes().size());
            postUpdate();
        }
        
    }



    private void postUpdate()
    {
        if(m_shutdown)
            return;
        
        synchronized(m_mutex)
        {
            ILobbyGameController controller = (ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
            controller.updateGame(m_gameID, (GameDescription) m_gameDescription.clone());
        }
    }



    public void shutDown()
    {
        m_shutdown = true;
        m_messenger.shutDown();
        m_gameMessenger.removeConnectionChangeListener(m_connectionChangeListener);
        cleanUpGameModelListener();
    }
    
    public void setGameStatus(GameDescription.GameStatus status)
    {
        synchronized(m_mutex)
        {
            m_gameDescription.setStatus(status);
            postUpdate();
        }
    }
    
    public String getComments()
    {
        return m_gameDescription.getComment();
    }
    
    public void setGameComments(String comments)
    {
        synchronized(m_mutex)
        {
            m_gameDescription.setComment(comments);
            postUpdate();
        }
    }

}
