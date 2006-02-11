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

import games.strategy.engine.EngineVersion;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.IServerReady;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.*;

import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.*;
import games.strategy.net.*;

import java.awt.Component;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.prefs.Preferences;

import javax.swing.*;

public class ClientModel implements IMessengerErrorListener
{
    private static Logger s_logger = Logger.getLogger(ClientModel.class.getName());
    
    private RemoteModelListener m_listener = RemoteModelListener.NULL_LISTENER;
    private IChannelMessenger m_channelMessenger;
    private IRemoteMessenger m_remoteMessenger;
    private IMessenger m_messenger;
    private final GameObjectStreamFactory m_objectStreamFactory = new GameObjectStreamFactory(null);
    private final GameSelectorModel m_gameSelectorModel;
    private final SetupPanelModel m_typePanelModel;
    private Component m_ui;
    private ChatPanel m_chatPanel;
    
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
    
    public void setRemoteModelListener(RemoteModelListener listener)
    {
        if(listener == null)
            listener = RemoteModelListener.NULL_LISTENER;
        
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
        String playername = prefs.get(ServerModel.PLAYERNAME, "Client");

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
        if (port >= 65536 || port == 0)
        {
            JOptionPane.showMessageDialog(ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String address = props.getHost();

        try
        {
            m_messenger = new ClientMessenger(address, port, name, m_objectStreamFactory);
        } catch (Exception ioe)
        {
            ioe.printStackTrace();
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
        
        if(!checkVersion(players, ui))
        {
            //clean up
            cancel();
            return false;
        }
        
        internalePlayerListingChanged(players);
        
        if(!serverStartup.isGameStarted(m_messenger.getLocalNode()))
        {
            m_remoteMessenger.unregisterRemote(ServerModel.getObserverWaitingToStartName(m_messenger.getLocalNode()));
        }
        
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
        
        m_messenger.shutDown();
        m_gameSelectorModel.setGameData(m_gameDataOnStartup);
        m_messenger.removeErrorListener(this);
        m_messenger = null;
        
    }
    
    
    private boolean checkVersion(PlayerListing msg, Component ui)
    {
      if(!msg.getEngineVersion().equals(EngineVersion.VERSION))
      {
        
          StringBuilder version = new StringBuilder();
          version.append("Error, server running different engine version.  Cannot join game. \n");
          version.append("Server\n engineVersion:").append(msg.getEngineVersion());
          version.append("\nClient\n engineVersion:").append(EngineVersion.VERSION);
    
          JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(ui), version.toString(), "Error", JOptionPane.ERROR_MESSAGE);
          return false;
        
      }
      return true;
    }
    
    
    
    private IClientChannel m_channelListener = new IClientChannel()
    {

      public void playerListingChanged(PlayerListing listing)
      {
          internalePlayerListingChanged(listing);
      }

      public void gameReset()
      {
          MainFrame.getInstance().setVisible(true);
      }
      
      public void doneSelectingPlayers(byte[] gameData, Map<String, INode> players)
      {
         startGame(gameData, players);
      }

        
    };
    
    IObserverWaitingToJoin m_observerWaitingToJoin = new IObserverWaitingToJoin()    
    {

        public void joinGame(byte[] gameData, Map<String, INode> players)
        {
            m_remoteMessenger.unregisterRemote(ServerModel.getObserverWaitingToStartName(m_messenger.getLocalNode()));
            startGame(gameData, players);
        }

        public void cannotJoinGame(String reason)
        {
            JOptionPane.showMessageDialog(m_ui, "Could not join game:" + reason);
            m_typePanelModel.showSelectType();
        }
        
    };
    
    
    private void startGame(byte[] gameData, Map<String, INode> players)
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

        final ClientGame clientGame = new ClientGame(data, playerSet, m_messenger,
                m_channelMessenger, m_remoteMessenger, new PlayerManager(players));

        
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
                
              data.getGameLoader().startGame(clientGame, playerSet);    
              
              //we will not have this remote if we are starting as an observer
              if(m_remoteMessenger.hasRemote(LauncherFrame.CLIENT_READY_CHANNEL))
                  ((IServerReady) m_remoteMessenger.getRemote(LauncherFrame.CLIENT_READY_CHANNEL)).clientReady();
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
        JOptionPane.showMessageDialog(m_ui,"Connection Lost", "Connection Lost", JOptionPane.ERROR_MESSAGE );
        m_typePanelModel.showSelectType();
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