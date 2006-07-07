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

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.data.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.IServerReady;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.ui.*;
import games.strategy.engine.framework.ui.background.WaitWindow;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.*;
import games.strategy.net.*;

import java.awt.Component;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.prefs.Preferences;

import javax.swing.*;

public class ClientModel implements IMessengerErrorListener
{
    
    public static final String CLIENT_READY_CHANNEL = "games.strategy.engine.framework.startup.mc.ClientModel.CLIENT_READY_CHANNEL";
    private static Logger s_logger = Logger.getLogger(ClientModel.class.getName());
    
    private IRemoteModelListener m_listener = IRemoteModelListener.NULL_LISTENER;
    private IChannelMessenger m_channelMessenger;
    private IRemoteMessenger m_remoteMessenger;
    private IMessenger m_messenger;
    private final GameObjectStreamFactory m_objectStreamFactory = new GameObjectStreamFactory(null);
    private final GameSelectorModel m_gameSelectorModel;
    private final SetupPanelModel m_typePanelModel;
    private Component m_ui;
    private ChatPanel m_chatPanel;
    private ClientGame m_game;
    private WaitWindow m_gameLoadingWindow = new WaitWindow("Loading game, please wait.");
    
    //we set the game data to be null, since we
    //are a client game, and the game data lives on the server
    //however, if we cancel, we want to restore the old game data.
    private GameData m_gameDataOnStartup;
    
    private Map<String,String> m_players = new HashMap<String,String>();

    ClientModel(GameSelectorModel gameSelectorModel, SetupPanelModel typePanelModel)
    {
        m_typePanelModel = typePanelModel;
        m_gameSelectorModel = gameSelectorModel;
    }
    
    public void setRemoteModelListener(IRemoteModelListener listener)
    {
        if(listener == null)
            listener = IRemoteModelListener.NULL_LISTENER;
        
        m_listener = listener;
    }
    
    
    private ClientProps getProps(Component ui)
    {
        if(System.getProperties().getProperty(GameRunner2.TRIPLEA_CLIENT_PROPERTY, "false").equals("true") &&
                System.getProperties().getProperty(GameRunner2.TRIPLEA_STARTED, "").equals("")     
           )
        {
            ClientProps props = new ClientProps();
            props.setHost(System.getProperty(GameRunner2.TRIPLEA_HOST_PROPERTY));
            props.setName(System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY));
            props.setPort(Integer.parseInt(System.getProperty(GameRunner2.TRIPLEA_PORT_PROPERTY)));
            
            System.setProperty(GameRunner2.TRIPLEA_STARTED, "true");
            return props;
        }
             
        
        //load in the saved name!
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        String playername = prefs.get(ServerModel.PLAYERNAME, System.getProperty("user.name"));

        ClientOptions options = new ClientOptions(ui, playername, GameRunner.PORT, "127.0.0.1");
        options.setLocationRelativeTo(ui);
        options.setVisible(true);
        options.dispose();

        
        if (!options.getOKPressed())
        {
            return null;
        }
        ClientProps props = new ClientProps();
        props.setHost(options.getAddress());
        props.setName(options.getName());
        props.setPort(options.getPort());
        
        return props;
    }
    
    
    public boolean createClientMessenger(Component ui)
    {
        m_gameDataOnStartup = m_gameSelectorModel.getGameData();
        
        ui = JOptionPane.getFrameForComponent(ui);
        m_ui = ui;
        
        //load in the saved name!
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());

        ClientProps props = getProps(ui);
        if(props == null)
            return false;

        String name = props.getName();
        s_logger.log(Level.FINE, "Client playing as:" + name);
        //save the name! -- lnxduk
        prefs.put(ServerModel.PLAYERNAME, name);

        int port = props.getPort();
        if (port >= 65536 || port <= 0)
        {
            JOptionPane.showMessageDialog(ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String address = props.getHost();

        try
        {
            m_messenger = new ClientMessenger(address, port, name, m_objectStreamFactory, new ClientLogin(m_ui));
            
        }catch (CouldNotLogInException ioe)
        {
            //an error message should have already been reported
            return false;
        }
        catch (Exception ioe)
        {
            ioe.printStackTrace(System.out);
            JOptionPane.showMessageDialog(ui, "Unable to connect:" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        m_messenger.addErrorListener(this);
        
        UnifiedMessenger unifiedMessenger = new UnifiedMessenger(m_messenger);
        m_channelMessenger = new ChannelMessenger(unifiedMessenger);
        m_remoteMessenger = new RemoteMessenger(unifiedMessenger);
        
        if(!m_channelMessenger.hasChannel(IClientChannel.CHANNEL_NAME ))
            m_channelMessenger.createChannel(IClientChannel.class, IClientChannel.CHANNEL_NAME );
        
        m_channelMessenger.registerChannelSubscriber(m_channelListener, IClientChannel.CHANNEL_NAME);
        m_chatPanel = new ChatPanel(m_messenger, m_channelMessenger, m_remoteMessenger, ServerModel.CHAT_NAME);        
        
        m_remoteMessenger.registerRemote(IObserverWaitingToJoin.class, m_observerWaitingToJoin, ServerModel.getObserverWaitingToStartName(m_messenger.getLocalNode()));
        //save this, it will be cleared later
        m_gameDataOnStartup = m_gameSelectorModel.getGameData();
        
        m_remoteMessenger.waitForRemote(ServerModel.SERVER_REMOTE_NAME, 1000);
        IServerStartupRemote serverStartup = getServerStartup();
        PlayerListing players = serverStartup.getPlayerListing();
        
        internalePlayerListingChanged(players);
        
        if(!serverStartup.isGameStarted(m_messenger.getLocalNode()))
        {
            m_remoteMessenger.unregisterRemote(ServerModel.getObserverWaitingToStartName(m_messenger.getLocalNode()));
        }
        
        m_gameSelectorModel.setCanSelect(false);
        return true;
    }

    private IServerStartupRemote getServerStartup()
    {
        return (IServerStartupRemote) m_remoteMessenger.getRemote(ServerModel.SERVER_REMOTE_NAME);
    }
    
    public void cancel()
    {
        if(m_messenger == null)
            return;
        
        m_objectStreamFactory.setData(null);
        m_messenger.shutDown();
        m_chatPanel.setChat(null);
        m_gameSelectorModel.setGameData(m_gameDataOnStartup);
        m_gameSelectorModel.setCanSelect(true);
        m_messenger.removeErrorListener(this);
    }
    
    private IClientChannel m_channelListener = new IClientChannel()
    {

      public void playerListingChanged(PlayerListing listing)
      {
          internalePlayerListingChanged(listing);
      }

      public void gameReset()
      {
          m_objectStreamFactory.setData(null);
          MainFrame.getInstance().setVisible(true);
      }
      
      public void doneSelectingPlayers(byte[] gameData, Map<String, INode> players)
      {
         CountDownLatch latch = new CountDownLatch(1);
         startGame(gameData, players, latch);
         
         try
         {
             latch.await(10, TimeUnit.SECONDS );
         } catch (InterruptedException e)
         {
             e.printStackTrace();
         }

         
      }

        
    };
    
    IObserverWaitingToJoin m_observerWaitingToJoin = new IObserverWaitingToJoin()    
    {

        public void joinGame(byte[] gameData, Map<String, INode> players)
        {
            m_remoteMessenger.unregisterRemote(ServerModel.getObserverWaitingToStartName(m_messenger.getLocalNode()));
            CountDownLatch latch = new CountDownLatch(1);
            startGame(gameData, players, latch);
            try
            {
                latch.await(10, TimeUnit.SECONDS );
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        public void cannotJoinGame(final String reason)
        {
            SwingUtilities.invokeLater(new Runnable()
            {
            
                public void run()
                {
                    m_typePanelModel.showSelectType();
                    JOptionPane.showMessageDialog(m_ui, "Could not join game:" + reason);
                }
            
            });
        }
        
    };
    
    
    
    private void startGame(final byte[] gameData, final Map<String, INode> players, final CountDownLatch onDone)
    {
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_gameLoadingWindow.setVisible(true);
                m_gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(m_ui));
                m_gameLoadingWindow.showWait();
            }
        
        });
        
        
        Runnable r = new Runnable()
        {
        
            public void run()
            {           
                try
                {
                    startGameInNewThread(gameData, players);
                } catch(RuntimeException e)
                {
                    m_gameLoadingWindow.doneWait();
                    throw e;
                }
                finally
                {
                    if(onDone != null)
                        onDone.countDown();
                }
            }
        
        };
        Thread t = new Thread(r);
        
        
        
        t.start();
    }
    
    
    private void startGameInNewThread(byte[] gameData, Map<String, INode> players)
    {
        final GameData data;
        try
        {
          data  = new GameDataManager().loadGame(new ByteArrayInputStream(gameData));
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
          return;
        }
        m_objectStreamFactory.setData(data);

        
        Map<String, String> playerMapping = new HashMap<String,String>();
        for(String player : m_players.keySet())
        {
            String playedBy = m_players.get(player);
            if(playedBy.equals(m_messenger.getLocalNode().getName()))
            {
                playerMapping.put(player, IGameLoader.CLIENT_PLAYER_TYPE);
            }
        }        
        
        final Set<IGamePlayer> playerSet = data.getGameLoader().createPlayers(playerMapping);

        Messengers messengers = new Messengers(m_messenger, m_remoteMessenger, m_channelMessenger);
        m_game  = new ClientGame(data, playerSet, new PlayerManager(players), messengers);

        
        Thread t = new Thread("Client Game Launcher")
        {
            public void run()
            {
              SwingUtilities.invokeLater(new Runnable()
              {
                  public void run()
                  {
                      JOptionPane.getFrameForComponent(m_ui).setVisible(false);
                  }
              
              });
              
              try
              {
                  //game will be null if we loose the connection
                  if(m_game != null)
                      data.getGameLoader().startGame(m_game, playerSet);    
                  
                  //we will not have this remote if we are starting as an observer
                  if(m_remoteMessenger.hasRemote(CLIENT_READY_CHANNEL))
                      ((IServerReady) m_remoteMessenger.getRemote(CLIENT_READY_CHANNEL)).clientReady();
              }
              finally
              {
                  m_gameLoadingWindow.doneWait();
              }
              
            }
        };
        t.start();
            
    }
    
    
    public void takePlayer(String playerName)
    {
        getServerStartup().takePlayer(m_messenger.getLocalNode(), playerName);
    }
    
    public void releasePlayer(String playerName)
    {
        getServerStartup().releasePlayer(m_messenger.getLocalNode(), playerName);
    }

    private void internalePlayerListingChanged(final PlayerListing listing)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_gameSelectorModel.clearDataButKeepGameInfo(listing.getGameName(), listing.getGameRound(), listing.getGameVersion().toString());
            }
        
        });

        synchronized(this)
        {
            m_players = listing.getPlayerListing();
        }
        
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_listener.playerListChanged();
            }
        
        });
        
    }
    
    public Map<String, String> getPlayerMapping()
    {
        synchronized(this)
        {
            return new HashMap<String,String>(m_players);
        }
    }
    
    public IMessenger getMessenger()
    {
        return m_messenger;
    }

    private void connectionLost()
    {
        JOptionPane.showMessageDialog(m_ui," Connection To Server Lost", "Connection Lost", JOptionPane.ERROR_MESSAGE );
        if(m_game != null)
        {
            m_game.shutDown();
            m_game = null;
        }
        
        MainFrame.getInstance().clientLeftGame();
    }
    
    public void connectionLost(INode node, Exception reason, List unsent)
    {
        connectionLost();
    }

    public void messengerInvalid(IMessenger messenger, Exception reason, List unsent)
    {
        connectionLost();
    }
    
    public ChatPanel getChatPanel()
    {
        return m_chatPanel;
    }



    
}


class ClientProps
{
    private int port;
    private String name;
    private String host;
    
    public String getHost()
    {
        return host;
    }
    public void setHost(String host)
    {
        this.host = host;
    }
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public int getPort()
    {
        return port;
    }
    public void setPort(int port)
    {
        this.port = port;
    }
    
    
    
}