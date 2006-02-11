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
import games.strategy.engine.chat.*;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.*;
import games.strategy.engine.framework.ui.*;
import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.util.Version;

import java.awt.Component;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

public class ServerModel extends Observable implements IMessengerErrorListener
{
    
    public static final String CHAT_NAME = "games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME";
    public static final String SERVER_REMOTE_NAME = "games.strategy.engine.framework.ui.ServerStartup.SERVER_REMOTE";

    
    
    public static String getObserverWaitingToStartName(INode node)
    {
        return "games.strategy.engine.framework.startup.mc.ServerModel.OBSERVER" + node.getName();
    }
    
    final static String PLAYERNAME = "PlayerName";
    private static Logger s_logger = Logger.getLogger(ServerModel.class.getName());
    
    private final GameObjectStreamFactory m_objectStreamFactory = new GameObjectStreamFactory(null);
    private final SetupPanelModel m_typePanelModel;
    private IServerMessenger m_serverMessenger;
    private IRemoteMessenger m_remoteMessenger;
    private IChannelMessenger m_channelMessenger;
    private GameData m_data;
    
    private Map<String, String> m_players  = new HashMap<String, String>();
    private RemoteModelListener m_listener = RemoteModelListener.NULL_LISTENER;
    private final GameSelectorModel m_gameSelectorModel;
    private Component m_ui;
    private ChatPanel m_chatPanel;
    private boolean m_inGame = false;
    private ServerLauncher m_serverLauncher;
    
    private Observer m_gameSelectorObserver = new Observer()
    {
    
        public void update(Observable o, Object arg)
        {
            gameDataChanged();
        }
    };
    
    
    
    
    
    public ServerModel(GameSelectorModel gameSelectorModel, SetupPanelModel typePanelModel)
    {
        m_gameSelectorModel = gameSelectorModel;
        m_typePanelModel = typePanelModel;
        m_gameSelectorModel.addObserver(m_gameSelectorObserver);
    }
    
    public void cancel()
    {
        m_gameSelectorModel.deleteObserver(m_gameSelectorObserver);
        m_serverMessenger.shutDown();
        m_serverMessenger.removeErrorListener(this);
    }
    
    public void setRemoteModelListener(RemoteModelListener listener)
    {
        if(listener == null)
            listener = RemoteModelListener.NULL_LISTENER;
        
        m_listener = listener;
    }
        
    private void gameDataChanged()
    {
        synchronized(this)
        {
            m_data = m_gameSelectorModel.getGameData();
            if(m_data != null)
            {
                m_players = new HashMap<String,String>();
                for(String name :  m_data.getPlayerList().getNames())
                {
                    m_players.put(name, m_serverMessenger.getLocalNode().getName());
                }
            }
            m_objectStreamFactory.setData(m_data);
        }
        
        notifyChanellPlayersChanged();
        m_listener.playerListChanged();
    }

    
    private ServerProps getServerProps(Component ui)
    {
        if(System.getProperties().getProperty(GameRunner2.TRIPLEA_SERVER_PROPERTY, "false").equals("true") &&
                System.getProperties().getProperty(GameRunner2.TRIPLEA_STARTED, "").equals("")     
           )
        {
            ServerProps props = new ServerProps();
            props.setName(System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY));
            props.setPort(Integer.parseInt(System.getProperty(GameRunner2.TRIPLEA_PORT_PROPERTY)));
            
            System.setProperty(GameRunner2.TRIPLEA_STARTED, "true");
            return props;
        }
             
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        String playername = prefs.get(PLAYERNAME, "Server");
       
        ServerOptions options = new ServerOptions(ui, playername, GameRunner.PORT);
        options.setLocationRelativeTo(ui);

        options.setVisible(true);
        options.dispose();

        if (!options.getOKPressed())
        {           
            return null;
        }

        String name = options.getName();
        s_logger.log(Level.FINE, "Server playing as:" + name);
        //save the name! -- lnxduk
        prefs.put(PLAYERNAME, name);

        int port = options.getPort();
        if (port >= 65536 || port == 0)
        {
            JOptionPane.showMessageDialog(ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
       
        ServerProps props = new ServerProps();
        
        props.setName(options.getName());
        props.setPort(options.getPort());
        
        return props;
    }
    
    /**
     * UI can be null.  We use it as the parent for message dialogs we show.
     * If you have a component displayed, use it. 
     */
    public boolean createServerMessenger(Component ui)
    {
        ui = JOptionPane.getFrameForComponent(ui);
        m_ui = ui;
        

        ServerProps props = getServerProps(ui);
        if(props == null)
            return false;

        try
        {
            m_serverMessenger = new ServerMessenger(props.getName(), props.getPort(), m_objectStreamFactory);
            m_serverMessenger.setAcceptNewConnections(true);
            m_serverMessenger.addErrorListener(this);
            UnifiedMessenger unifiedMessenger = new UnifiedMessenger(m_serverMessenger);
            
            
            m_remoteMessenger = new RemoteMessenger(unifiedMessenger);
            m_remoteMessenger.registerRemote(IServerStartupRemote.class, m_serverStartupRemote, SERVER_REMOTE_NAME);
            
            m_channelMessenger = new ChannelMessenger(unifiedMessenger);
            m_channelMessenger.createChannel(IClientChannel.class, IClientChannel.CHANNEL_NAME);            
            
            ChatController m_chatController = new ChatController(CHAT_NAME,m_serverMessenger, m_remoteMessenger, m_channelMessenger);
            m_chatPanel = new ChatPanel(m_serverMessenger, m_channelMessenger, m_remoteMessenger, CHAT_NAME);
            
            gameDataChanged();
            
            return true;
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
            JOptionPane.showMessageDialog(ui, "Unable to create server socket:" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
    
    private IServerStartupRemote m_serverStartupRemote = new IServerStartupRemote()
    {

      public PlayerListing getPlayerListing()
      {
          return getPlayerListingInternal();
      }

      public void takePlayer(INode who, String playerName)
      {
          takePlayerInternal(who, true, playerName);
      }

      public void releasePlayer(INode who, String playerName)
      {
          takePlayerInternal(who, false, playerName);
      }

      public boolean isGameStarted(INode newNode)
      {
          if(m_inGame)
          {
              IObserverWaitingToJoin observerWaitingToJoin = (IObserverWaitingToJoin) m_remoteMessenger.getRemote(getObserverWaitingToStartName(newNode));
              m_serverLauncher.addObserver(observerWaitingToJoin);
              return true;
          }
          else
          {
              return false;
          }
      }

    };
    
    
    
    
    
    private PlayerListing getPlayerListingInternal()
    {
      synchronized(this)
      {
          if(m_data == null)
              return new PlayerListing(new HashMap<String,String>(), EngineVersion.VERSION, new Version(0,0), m_gameSelectorModel.getGameName(), m_gameSelectorModel.getGameRound());
          else
              return new PlayerListing(new HashMap<String,String>(m_players), EngineVersion.VERSION, m_data.getGameVersion(), m_data.getGameName(), m_data.getSequence().getRound() + "");
      }
    }
    
    private void takePlayerInternal(INode from, boolean take, String playerName)
    {
      //synchronize to make sure two adds arent executed at once
      synchronized(this)
      {
          if(!m_players.containsKey(playerName))
              return;
          
          if(take)
              m_players.put(playerName, from.getName());
          else
              m_players.put(playerName, null);
      }
      
      notifyChanellPlayersChanged();          
      m_listener.playersTakenChanged();
      
     }

    private void notifyChanellPlayersChanged()
    {
        IClientChannel channel = (IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
        channel.playerListingChanged(getPlayerListingInternal());
    }
    
    public void takePlayer(String playerName)
    {
        takePlayerInternal(m_serverMessenger.getLocalNode(), true, playerName);
    }
    
    public void releasePlayer(String playerName)
    {
        takePlayerInternal(m_serverMessenger.getLocalNode(), false, playerName);
    }
    
    public IServerMessenger getMessenger()
    {
        return m_serverMessenger; 
    }
    
    public Map<String,String> getPlayers()
    {
        synchronized(this)
        {
            return new HashMap<String,String>(m_players);
        }
    }
    

    public void messengerInvalid(IMessenger messenger, Exception reason,
            List unsent)
    {
        JOptionPane.showMessageDialog(m_ui, "Connection lost", "Error", JOptionPane.ERROR_MESSAGE );
        m_typePanelModel.showSelectType();
    }

    public void connectionLost(INode node, Exception reason, List unsent)
    {
        //will be handled elsewhere
        if(m_inGame)
        {
            m_serverLauncher.connectionLost(node);
            return;
        }
        
        //we lost a node.  Remove the players he plays.
        
        List<String> free = new ArrayList<String>();
        synchronized(this)
        {
            for(String player : m_players.keySet())
            {
                String playedBy = m_players.get(player);
                if(playedBy.equals(node.getName()))
                {
                    free.add(player);
                }
            }
        }
        
        for(String player : free)
        {
            takePlayerInternal(node, false, player);
        }
        
    }

    public ChatPanel getChatPanel()
    {
        return m_chatPanel;
    }

    public ILauncher getLauncher()
    {
        synchronized(this)
        {
            //-1 since we dont count outselves
            int clientCount = m_serverMessenger.getNodes().size() -1;
            Map<String,String> localPlayerMappings = new HashMap<String,String>();
            Map<String,INode> remotePlayers = new HashMap<String,INode>();
            
            String localPlayerType = m_data.getGameLoader().getServerPlayerTypes()[0];
            
            for(String player : m_players.keySet() )
            {
                String playedBy = m_players.get(player);
                if(playedBy.equals( m_serverMessenger.getLocalNode().getName()))
                {
                    localPlayerMappings.put(player, localPlayerType);
                }
                else
                {
                    Set<INode> nodes = m_serverMessenger.getNodes();
                    for(INode node : nodes)
                    {
                        if(node.getName().equals(playedBy))
                        {
                            remotePlayers.put(player, node);
                            break;
                        }
                    }
                }
            }
            
            ServerLauncher launcher = new ServerLauncher(clientCount, m_remoteMessenger, m_channelMessenger, m_serverMessenger, m_gameSelectorModel, localPlayerMappings, remotePlayers, this);
            return launcher;
        }
    }

    public void newGame()
    {
        m_serverMessenger.setAcceptNewConnections(true);
        IClientChannel channel = (IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
        notifyChanellPlayersChanged();
        channel.gameReset();
    }
    
    public void setInGame(boolean aBool, ServerLauncher launcher)
    {
        m_serverLauncher = launcher;
        m_inGame = aBool;
    }
    
}


class ServerProps
{
    
    private String name;
    private int port;
    
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
