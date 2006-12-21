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

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.ILobbyGameController;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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

    private IGame m_game;
    private GameStepListener m_gameStepListener = new GameStepListener()
    {
        
        public void gameStepChanged(String stepName, String delegateName, PlayerID player, int round, String displayName)
        {
            InGameLobbyWatcher.this.gameStepChanged(stepName, round);
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
    public static InGameLobbyWatcher newInGameLobbyWatcher(IServerMessenger gameMessenger, JComponent parent)
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
            return new InGameLobbyWatcher(messenger, rm, gameMessenger, parent);
        }  catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }


    public void setGame(IGame game)
    {
        if(m_game != null)
        {
            m_game.removeGameStepListener(m_gameStepListener);
        }
        
        m_game = game;
        
        if(game != null)
        {
            game.addGameStepListener(m_gameStepListener);
            gameStepChanged(game.getData().getSequence().getStep().getName(),  game.getData().getSequence().getRound());
        }
    }
    
    private void gameStepChanged(String stepName, int round)
    {
        synchronized(m_mutex)
        {
            if(!m_gameDescription.getRound().equals(Integer.toString(round)))
            {
                m_gameDescription.setRound(round + "");
            }
            postUpdate();
        }
    }

    
    
    private void gameSelectorModelUpdated()
    {
        synchronized(m_mutex)
        {
            m_gameDescription.setGameName(m_gameSelectorModel.getGameName());
            postUpdate();
        }
        
    }



    public InGameLobbyWatcher(final IMessenger messenger, final IRemoteMessenger remoteMessenger, final IServerMessenger serverMessenger, final JComponent parent)
    {
        m_messenger = messenger;
        m_remoteMessenger = remoteMessenger;
        
        m_gameMessenger = serverMessenger;
        
        m_gameDescription = new GameDescription(m_messenger.getLocalNode(), m_gameMessenger.getLocalNode().getPort(), new Date(), "???", 1, GameStatus.WAITING_FOR_PLAYERS, "-", m_gameMessenger.getLocalNode().getName(), System.getProperty(GameRunner2.LOBBY_GAME_COMMENTS));
        
        final ILobbyGameController controller = (ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
        synchronized(m_mutex)
        {

            controller.postGame(m_gameID, (GameDescription) m_gameDescription.clone());
        }
        
        //if we loose our connection, then shutdown
        m_messenger.addErrorListener(new IMessengerErrorListener()
        {
        
            public void messengerInvalid(IMessenger messenger, Exception reason)
            {
                shutDown();
            }

        
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
        
        Runnable r = new Runnable()
        {
            public void run()
            {
              //if the server cannot connect to us, then quit  
              if(!controller.testGame(m_gameID)) 
              {  
                  if(isActive()) 
                  {
                      shutDown();
                      SwingUtilities.invokeLater(new Runnable()
                      {
                    
                         public void run()
                         {
                             String message = "Your computer is not reachable from the internet.\n" +
                                              "Please check your firewall or router configuration.";
                             
                            JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(parent),  message, "Could Not Host", JOptionPane.ERROR_MESSAGE);
                            System.exit(-1);
                         }
                    
                      });
                  }
              }
            }
        };
        
        new Thread(r).start();
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
    
    public boolean isActive()
    {
        return !m_shutdown;
    }
    
    public void setGameStatus(GameDescription.GameStatus status, IGame game)
    {
        synchronized(m_mutex)
        {
            m_gameDescription.setStatus(status);
            
            if(game == null)
            {
                m_gameDescription.setRound("-");
            }
            else
            {
                m_gameDescription.setRound(game.getData().getSequence().getRound() + "");
            }
            setGame(game);
            
            
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
