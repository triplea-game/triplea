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
 * ServerStartup.java
 *
 * Created on February 1, 2002, 11:48 AM
 */

package games.strategy.engine.framework.ui;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.data.*;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.util.Version;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * UI For starting server games.
 *
 * @author  Sean Bridges
 */
public class ServerStartup extends JPanel
{
  private LauncherFrame m_launcher;
  private IGameLoader m_loader;
  private final IServerMessenger m_messenger;
  private final IRemoteMessenger m_remoteMessenger;
  private final IChannelMessenger m_channelMessenger;
  private GameData m_data;

  private JTextField m_portField;
  private JTextField m_addressField;
  private JTextField m_nameField;

  //list of PlayerRows

  private List<PlayerRow> m_playerRows = Collections.emptyList();

  private JPanel m_info = new JPanel();
  
  public static final String SERVER_REMOTE_NAME = "games.strategy.engine.framework.ui.ServerStartup.SERVER_REMOTE";
  
  /**
   * Creates a new instance of ServerStartup
   */
  public ServerStartup(IServerMessenger messenger, IRemoteMessenger remoteMessenger, IChannelMessenger channelmessenger)
  {
    m_messenger = messenger;
    m_remoteMessenger = remoteMessenger;
    m_remoteMessenger.registerRemote(IServerStartupRemote.class, m_serverStartupRemote, SERVER_REMOTE_NAME);
    m_channelMessenger = channelmessenger;
    
    m_channelMessenger.createChannel(IClientChannel.class, IClientChannel.CHANNEL_NAME);
    
    initComponents();
    layoutComponents();
    setWidgetActivation();
  }

  public void cleanUp()
  {
      m_remoteMessenger.unregisterRemote(SERVER_REMOTE_NAME);
  }
  
  public boolean allPlayersFilled()
  {
    Iterator<PlayerRow> iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row =  iter.next();
      if(!row.isPlayed())
        return false;
    }
    return true;
  }

  public void setGameData(GameData data)
  {
    m_loader = data.getGameLoader();
    m_data = data;

    updateGamePlayers();
    layoutPlayers();
    IClientChannel channel = (IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
    channel.playerListingChanged(getPlayerListingInternal());
    
  }

  public void setLauncerFrame(LauncherFrame frame)
  {
    m_launcher = frame;
  }

  /**
   * @return a mapping of playerName -> local player type.
   */
  public Map<String, String> getLocalPlayerMapping()
  {
    Map<String, String> map = new HashMap<String, String>();

    Iterator<PlayerRow> iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row = iter.next();
      if(row.isLocal())
      {
        map.put(row.getPlayerName(), row.getLocalType());
      }
    }
    return map;
  }

  /**
   * @return a mapping of playerName -> remoteNode.
   */
  public Map<String, INode> getRemotePlayerMapping()
  {
    Map<String, INode> map = new HashMap<String, INode>();

    Iterator<PlayerRow> iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row = iter.next();
      if(!row.isLocal())
      {
        map.put(row.getPlayerName(), row.getNode());
      }
    }
    return map;
  }

  public void waitForPlayers()
  {
    m_messenger.addErrorListener(m_messengerErrorListener);

    m_messenger.setConnectionAccepter(m_connectionAccepter);
    m_messenger.setAcceptNewConnections(true);

  }

  public void cleanUpWaitForPlayers()
  {
    m_messenger.setAcceptNewConnections(false);
    m_messenger.setConnectionAccepter(null);

   
    m_messenger.removeErrorListener(m_messengerErrorListener);

  }

  private void initComponents()
  {
    m_portField = new JTextField("" + m_channelMessenger.getLocalNode().getPort());
    m_portField.setEnabled(false);
    m_portField.setColumns(6);

    m_addressField = new JTextField(m_channelMessenger.getLocalNode().getAddress().getHostAddress());
    m_addressField.setEnabled(false);
    m_addressField.setColumns(20);

    m_nameField = new JTextField(m_channelMessenger.getLocalNode().getName());
    m_nameField.setEnabled(false);
    m_nameField.setColumns(20);
  }

  private void updateGamePlayers()
  {
    String[] localOptions = m_loader.getServerPlayerTypes();

    m_playerRows = new ArrayList<PlayerRow>();
    Iterator iter = m_data.getPlayerList().getPlayers().iterator();

    while(iter.hasNext())
    {
      PlayerID id = (PlayerID) iter.next();
      m_playerRows.add(new PlayerRow(id.getName(), localOptions[0]));
    }
  }

  private void layoutComponents()
  {
    setLayout(new BorderLayout());


    m_info.setLayout(new GridBagLayout());

    m_info.add(new JLabel("Name:"), new GridBagConstraints(0,0,1,1,0.5,0.5,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,0,0,0), 0,0));
    m_info.add(new JLabel("Address:"), new GridBagConstraints(0,1,1,1,0.5,0.5,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,0,0,0), 0,0));
    m_info.add(new JLabel("Port:"), new GridBagConstraints(0,2,1,1,0.5,0.5,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,0,0,0), 0,0));

    m_info.add(m_nameField, new GridBagConstraints(1,0,1,1,0.5,0.5,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,0,0,0), 0,0));
    m_info.add(m_addressField, new GridBagConstraints(1,1,1,1,0.5,0.5,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,0,0,0), 0,0));
    m_info.add(m_portField, new GridBagConstraints(1,2,1,1,0.5,0.5,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5,0,0,0), 0,0));

    add(m_info, BorderLayout.NORTH);

    JPanel lowerPanel = new JPanel();
    lowerPanel.setLayout(new BorderLayout());
    lowerPanel.add(new JLabel(" "), BorderLayout.NORTH);

  }

  private void layoutPlayers()
  {
    JPanel players = new JPanel();
    GridBagLayout layout = new GridBagLayout();
    players.setLayout(layout);

    Insets spacing = new Insets(3,23,0,0);
    Insets lastSpacing = new Insets(3,23,0,23);

    GridBagConstraints nameConstraints = new GridBagConstraints();
    nameConstraints.anchor = GridBagConstraints.WEST;
    nameConstraints.gridx = 0;
    nameConstraints.insets = spacing;

    GridBagConstraints playerConstraints = new GridBagConstraints();
    playerConstraints.anchor = GridBagConstraints.WEST;
    playerConstraints.gridx = 1;
    playerConstraints.insets = spacing;

    GridBagConstraints localConstraints = new GridBagConstraints();
    localConstraints.anchor = GridBagConstraints.WEST;
    localConstraints.gridx = 2;
    localConstraints.insets = lastSpacing;

    JLabel nameLabel = new JLabel("Name");
    nameLabel.setForeground(Color.black);
    layout.setConstraints(nameLabel, nameConstraints);
    players.add(nameLabel);

    JLabel playedByLabel = new JLabel("Played by");
    playedByLabel.setForeground(Color.black);
    layout.setConstraints(playedByLabel, playerConstraints);
    players.add(playedByLabel);

    JLabel localLabel = new JLabel("Local");
    localLabel.setForeground(Color.black);
    layout.setConstraints(localLabel, localConstraints);
    players.add(localLabel);

    Iterator<PlayerRow> iter = m_playerRows.iterator();

    if(!iter.hasNext())
    {
      JLabel noPlayers = new JLabel("Load a game file first");
      layout.setConstraints(noPlayers, nameConstraints);
      players.add(noPlayers);
    }

    while(iter.hasNext())
    {
      PlayerRow row = iter.next();

      layout.setConstraints(row.getName(), nameConstraints);
      players.add(row.getName());

      layout.setConstraints(row.getPlayer(), playerConstraints);
      players.add(row.getPlayer());

      layout.setConstraints(row.getLocal(), localConstraints);
      players.add(row.getLocal());
    }

    removeAll();
    add(m_info, BorderLayout.NORTH);
    add(players, BorderLayout.CENTER);


  }


  private void setWidgetActivation()
  {
  }

  private PlayerListing getPlayerListingInternal()
  {
    HashMap<String, String> mapping = new HashMap<String, String>();

    if(m_data == null)
      return new PlayerListing(mapping, EngineVersion.VERSION, new Version(0,0), "","");

    Iterator<PlayerRow> iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row = iter.next();
      String name = row.getPlayerName();
      if(row.isLocal())
      {
        mapping.put(name, m_messenger.getLocalNode().getName());
      }
      else
      {
        INode node = row.getNode();
        if(node == null)
          mapping.put(name, null);
        else
          mapping.put(name, node.getName());
      }
    }

    return new PlayerListing(mapping, EngineVersion.VERSION, m_data.getGameVersion(), m_data.getGameName(), "");
  }

  private void takePlayerInternal(INode from, boolean take, String playerName)
  {
    //synchronize to make sure two adds arent executed at once
    synchronized(this)
    {
      

      Iterator<PlayerRow> iter = m_playerRows.iterator();
      while(iter.hasNext())
      {
        PlayerRow row = iter.next();
        if(!row.isLocal())
        {
          if(take)
          {
            if(row.getNode() == null && row.getPlayerName().equals(playerName))
            {
              row.setNode(from);
              IClientChannel channel = (IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
              channel.playerListingChanged(getPlayerListingInternal());

            }
          }
          else
          {
            if(row.getNode() != null && row.getNode().equals(from) && row.getPlayerName().equals(playerName))
            {
              row.setNode(null);
              IClientChannel channel = (IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
              channel.playerListingChanged(getPlayerListingInternal());

            }
          }
        }
      }//end while
    }//end synchronized
    setWidgetActivation();
    m_launcher.setWidgetActivation();
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

  };

 

  
  class PlayerRow
  {
    private JLabel m_nameLabel;
    private JLabel m_playerLabel;
    private JCheckBox m_localCheckBox;
    private INode m_node;
    private String m_localType;

    PlayerRow(String playerName, String localType)
    {
      m_nameLabel = new JLabel(playerName);
      m_playerLabel = new JLabel(m_messenger.getLocalNode().getName());
      m_localCheckBox = new JCheckBox();
      m_localCheckBox.addActionListener(m_actionListener);
      m_localCheckBox.setSelected(true);
      m_localType = localType;
    }

    public JLabel getName()
    {
      return m_nameLabel;
    }

    public JLabel getPlayer()
    {
      return m_playerLabel;
    }

    public String getPlayerName()
    {
      return m_nameLabel.getText();
    }

    public JCheckBox getLocal()
    {
      return m_localCheckBox;
    }

    public boolean isLocal()
    {
      return m_localCheckBox.isSelected();
    }

    public void setLocal(boolean local)
    {
      m_localCheckBox.setSelected(local);
    }

    public INode getNode()
    {
      return m_node;
    }

    public void setNode(INode node)
    {
      m_node = node;
      if(node != null)
        m_playerLabel.setText(node.getName());
      else
        m_playerLabel.setText("-");
    }

    public String getLocalType()
    {
      return m_localType;
    }

    public boolean isPlayed()
    {
      return ( !isLocal() && m_node != null) || isLocal();
    }

    private ActionListener m_actionListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        if(!isLocal())
          m_playerLabel.setText("-");

        else
          m_playerLabel.setText(m_messenger.getLocalNode().getName());

        m_node = null;
        setWidgetActivation();
        m_launcher.setWidgetActivation();
        IClientChannel channel = (IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
        channel.playerListingChanged(getPlayerListingInternal());

      }
    };
  }

  IConnectionAccepter m_connectionAccepter = new IConnectionAccepter()
  {
    public String acceptConnection(IServerMessenger messenger, INode node)
    {

      // Make sure the name is valid.  Game is used to display
      // game messages, want at least two chars,
      // the first of which is a letter to avoid confusion.
      String name = node.getName();
      if(name.length() < 2  || !Character.isLetter(name.charAt(0)) || name.equalsIgnoreCase("Game"))
        return name + "is an invalid name";

      return null;
    }
  };

  private  IMessengerErrorListener m_messengerErrorListener = new IMessengerErrorListener()
  {
    public void connectionLost(INode node, Exception reason, java.util.List unsent)
    {
      Iterator<PlayerRow> iter = m_playerRows.iterator();
      while(iter.hasNext())
      {
        PlayerRow row = iter.next();
        if(!row.isLocal() && row.getNode() != null && row.getNode().equals(node))
        {
          row.setNode(null);
        }
      }
      setWidgetActivation();
    }

    public void messengerInvalid(IMessenger messenger, Exception reason, java.util.List unsent)
    {
      JOptionPane.showMessageDialog(ServerStartup.this, "Unrecoverable error" + reason.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  };


}
