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

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


import games.strategy.engine.data.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.message.*;
import games.strategy.net.*;
import games.strategy.engine.EngineVersion;
import games.strategy.util.Version;

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
  private GameData m_data;

  private JTextField m_portField;
  private JTextField m_addressField;
  private JTextField m_nameField;

  //list of PlayerRows
  private List m_playerRows = Collections.EMPTY_LIST;

  private byte[] m_dataBytes;

  /**
   * Creates a new instance of ServerStartup
   */
  public ServerStartup(IServerMessenger messenger)
  {
    m_messenger = messenger;

    initComponents();
    layoutComponents();
    setWidgetActivation();
  }

  public boolean allPlayersFilled()
  {
    Iterator iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row =  (PlayerRow) iter.next();
      if(!row.isPlayed())
        return false;
    }
    return true;
  }

  public void setGameData(GameData data)
  {
    m_loader = data.getGameLoader();
    m_data = data;

    try
    {
      serializeGameData();
    } catch(IOException ioe)
    {
        ioe.printStackTrace();
      throw new RuntimeException(ioe.getMessage());
    }

    updateGamePlayers();
    layoutPlayers();
    m_messenger.broadcast(getPlayerListing());
  }

  public void setLauncerFrame(LauncherFrame frame)
  {
    m_launcher = frame;
  }

  /**
   * store the game data as a byte array so that we can send it to the
   * client
   */
  private void serializeGameData() throws IOException
  {
    ByteArrayOutputStream sink = new ByteArrayOutputStream(25000);

    ZipOutputStream zip = new ZipOutputStream(sink);
    zip.setLevel(9);
    zip.putNextEntry(new ZipEntry("gameData"));


    new GameDataManager().saveGame(zip, m_data);
    zip.flush();
    zip.closeEntry();
    zip.close();


    m_dataBytes = sink.toByteArray();


  }

  public byte[] getGameDataBytes()
  {
    return m_dataBytes;
  }

  /**
   * @return a mapping of playerName -> local player type.
   */
  public Map getLocalPlayerMapping()
  {
    Map map = new HashMap();

    Iterator iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row = (PlayerRow) iter.next();
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
  public Map getRemotePlayerMapping()
  {
    Map map = new HashMap();

    Iterator iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row = (PlayerRow) iter.next();
      if(!row.isLocal())
      {
        map.put(row.getPlayerName(), row.getNode());
      }
    }
    return map;
  }

  public void waitForPlayers()
  {
    m_messenger.addMessageListener(m_messageListener);
    m_messenger.addErrorListener(m_messengerErrorListener);

    m_messenger.setConnectionAccepter(m_connectionAccepter);
    m_messenger.setAcceptNewConnections(true);

  }

  public void cleanUpWaitForPlayers()
  {
    m_messenger.setAcceptNewConnections(false);
    m_messenger.setConnectionAccepter(null);

    m_messenger.removeMessageListener(m_messageListener);
    m_messenger.removeErrorListener(m_messengerErrorListener);

  }

  private void initComponents()
  {
    m_portField = new JTextField("" + m_messenger.getLocalNode().getPort());
    m_portField.setEnabled(false);
    m_portField.setColumns(6);

    m_addressField = new JTextField(m_messenger.getLocalNode().getAddress().getHostAddress());
    m_addressField.setEnabled(false);
    m_addressField.setColumns(20);

    m_nameField = new JTextField(m_messenger.getLocalNode().getName());
    m_nameField.setEnabled(false);
    m_nameField.setColumns(20);
  }

  private void updateGamePlayers()
  {
    String[] localOptions = m_loader.getServerPlayerTypes();

    m_playerRows = new ArrayList();
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

    JPanel info = new JPanel();
    info.setLayout(new GridBagLayout());

    info.add(new JLabel("Name:"), new GridBagConstraints(0,0,1,1,0.5,0.5,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
    info.add(new JLabel("Address:"), new GridBagConstraints(0,1,1,1,0.5,0.5,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
    info.add(new JLabel("Port:"), new GridBagConstraints(0,2,1,1,0.5,0.5,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));

    info.add(m_nameField, new GridBagConstraints(1,0,1,1,0.5,0.5,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
    info.add(m_addressField, new GridBagConstraints(1,1,1,1,0.5,0.5,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));
    info.add(m_portField, new GridBagConstraints(1,2,1,1,0.5,0.5,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0,0));



    add(info, BorderLayout.NORTH);

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

    Iterator iter = m_playerRows.iterator();

    if(!iter.hasNext())
    {
      JLabel noPlayers = new JLabel("Load a game file first");
      layout.setConstraints(noPlayers, nameConstraints);
      players.add(noPlayers);
    }

    while(iter.hasNext())
    {
      PlayerRow row = (PlayerRow) iter.next();

      layout.setConstraints(row.getName(), nameConstraints);
      players.add(row.getName());

      layout.setConstraints(row.getPlayer(), playerConstraints);
      players.add(row.getPlayer());

      layout.setConstraints(row.getLocal(), localConstraints);
      players.add(row.getLocal());
    }

    add(players, BorderLayout.CENTER);


  }


  private void setWidgetActivation()
  {
  }

  private PlayerListingMessage getPlayerListing()
  {
    HashMap mapping = new HashMap();

    if(m_data == null)
      return new PlayerListingMessage(mapping, EngineVersion.VERSION, new Version(0,0), "");

    Iterator iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row = (PlayerRow) iter.next();
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

    return new PlayerListingMessage(mapping, EngineVersion.VERSION, m_data.getGameVersion(), m_data.getGameName());
  }

  private void takePlayerMessageReceived(TakePlayerMessage msg, INode from)
  {
    //synchronize to make sure two adds arent executed at once
    synchronized(this)
    {
      String playerName = ((TakePlayerMessage) msg).getPlayerName();

      Iterator iter = m_playerRows.iterator();
      while(iter.hasNext())
      {
        PlayerRow row = (PlayerRow) iter.next();
        if(!row.isLocal())
        {
          if(msg.play())
          {
            if(row.getNode() == null && row.getPlayerName().equals(playerName))
            {
              row.setNode(from);
              m_messenger.broadcast(getPlayerListing());
            }
          }
          else
          {
            if(row.getNode() != null && row.getNode().equals(from) && row.getPlayerName().equals(playerName))
            {
              row.setNode(null);
              m_messenger.broadcast(getPlayerListing());
            }
          }
        }
      }//end while
    }//end synchronized
    setWidgetActivation();
    m_launcher.setWidgetActivation();
  }


  private IMessageListener m_messageListener = new IMessageListener()
  {
    public void messageReceived(Serializable msg, INode from)
    {
      if(msg instanceof ListPlayerRequest)
      {
        m_messenger.broadcast(getPlayerListing());
      }
      else if(msg instanceof TakePlayerMessage)
      {
         takePlayerMessageReceived( (TakePlayerMessage) msg, from);
      }
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
        m_messenger.broadcast(getPlayerListing());
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

      //  Make sure that only one name exists.
      Iterator iter = messenger.getNodes().iterator();
      while(iter.hasNext())
      {
        INode current = (INode) iter.next();
        if(current.getName().equalsIgnoreCase(name))
          return name + " is already in use";
      }
      return null;
    }
  };

  private  IMessengerErrorListener m_messengerErrorListener = new IMessengerErrorListener()
  {
    public void connectionLost(INode node, Exception reason, java.util.List unsent)
    {
      Iterator iter = m_playerRows.iterator();
      while(iter.hasNext())
      {
        PlayerRow row = (PlayerRow) iter.next();
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