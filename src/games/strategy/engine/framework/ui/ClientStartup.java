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
 * ClientStartup.java
 *
 * Created on February 1, 2002, 4:17 PM
 */

package games.strategy.engine.framework.ui;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.*;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 *
 * UI for starting the client.
 *
 * @author  Sean Bridges
 */
public class ClientStartup extends JPanel
{
  private static final Insets BUTTON_INSETS = new Insets(0,0,0,0);

  private final IChannelMessenger m_channelMessenger;
  private final IRemoteMessenger m_remoteMessenger;
  
  private JLabel m_nameLabel;
  private LauncherFrame m_launcher;
  private Set m_players = Collections.EMPTY_SET;

  //list of PlayerRows
  private List<PlayerRow> m_playerRows;

  /**
   * Creates a new instance of ServerStartup
   */
  public ClientStartup(IChannelMessenger channelMessenger, IRemoteMessenger remoteMessenger)
  {
    m_channelMessenger = channelMessenger;
    m_remoteMessenger = remoteMessenger;
  

    if(!m_channelMessenger.hasChannel(IClientChannel.CHANNEL_NAME ))
        m_channelMessenger.createChannel(IClientChannel.class, IClientChannel.CHANNEL_NAME );
    m_channelMessenger.registerChannelSubscriber(m_channelListener, IClientChannel.CHANNEL_NAME);
    
    initComponents();
    layoutComponents();
    setWidgetActivation();
  }

  public void refreshPlayers(PlayerListing message)
  {
    m_players = message.getPlayers();
    initComponents();

    Map listing = message.getPlayerListing();
    Iterator listings = listing.keySet().iterator();

    while(listings.hasNext())
    {
      String playerName = (String) listings.next();
      String node = (String) listing.get(playerName);

      Iterator<PlayerRow> rows = m_playerRows.iterator();
      while(rows.hasNext())
      {
        PlayerRow row =  rows.next();
        if(row.getPlayerName().equals(playerName))
        {
          row.setPlayerName(node);
        }
      }

    }



    layoutComponents();
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
      if(row.isPlaying())
      {
        map.put(row.getPlayerName(), row.getLocalType());
      }
    }
    return map;
  }

  public void waitForPlayers()
  {
      IServerStartupRemote server = getRemoteServer();
      PlayerListing listing = server.getPlayerListing();
      updatePlayerListing(listing); 
  }


  /**
 * @return
 */
private IServerStartupRemote getRemoteServer()
{
    IServerStartupRemote server = (IServerStartupRemote) m_remoteMessenger.getRemote(ServerStartup.SERVER_REMOTE_NAME);
    return server;
}

private void initComponents()
  {
    m_nameLabel = new JLabel("Name:" + m_channelMessenger.getLocalNode().getName());

    m_playerRows = new ArrayList<PlayerRow>();
    Iterator iter = m_players.iterator();

    while(iter.hasNext())
    {
      String name = (String) iter.next();
      m_playerRows.add(new PlayerRow(name, IGameLoader.CLIENT_PLAYER_TYPE));
    }
  }

  private void layoutComponents()
  {
    removeAll();
    setLayout(new BorderLayout());

    JPanel info = new JPanel();
    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
    info.add(m_nameLabel);
    info.add(new JLabel(" "));
    add(info, BorderLayout.NORTH);

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
    playerConstraints.gridx = 2;
    playerConstraints.insets = spacing;

    GridBagConstraints playConstraints = new GridBagConstraints();
    playConstraints.anchor = GridBagConstraints.WEST;
    playConstraints.gridx = 3;
    playConstraints.insets = lastSpacing;

    JLabel nameLabel = new JLabel("Name");
    nameLabel.setForeground(Color.black);
    layout.setConstraints(nameLabel, nameConstraints);
    players.add(nameLabel);

    JLabel playerLabel = new JLabel("Played By");
    playerLabel.setForeground(Color.black);
    layout.setConstraints(playerLabel, playerConstraints);
    players.add(playerLabel);

    JLabel playedByLabel = new JLabel("                    ");
    layout.setConstraints(playedByLabel, playConstraints);
    players.add(playedByLabel);

    Iterator<PlayerRow> iter = m_playerRows.iterator();
    while(iter.hasNext())
    {
      PlayerRow row = iter.next();

      layout.setConstraints(row.getName(), nameConstraints);
      players.add(row.getName());

      layout.setConstraints(row.getPlayer(), playerConstraints);
      players.add(row.getPlayer());

      layout.setConstraints(row.getPlayerComponent(), playConstraints);
      players.add(row.getPlayerComponent());
    }

    add(players, BorderLayout.CENTER);

    validate();
  }

  private void checkVersion(PlayerListing msg)
  {
    if(!msg.getEngineVersion().equals(EngineVersion.VERSION))
    {
      try
      {
        StringBuilder version = new StringBuilder();

        version.append("Error, server running different engine version.  Cannot join game. \n");

        version.append("Server\n engineVersion:").append(msg.getEngineVersion());

        version.append("\nClient\n engineVersion:").append(EngineVersion.VERSION);

        System.err.println(version.toString());

        JOptionPane.showMessageDialog(this, version.toString(), "Error", JOptionPane.ERROR_MESSAGE);
       
      } finally
      {
        System.exit(0);
      }
    }
  }

  private void setWidgetActivation()
  {

  }

  public void setLauncherFrame(LauncherFrame launcher)
  {
    m_launcher = launcher;
  }

  private IClientChannel m_channelListener = new IClientChannel()
  {

    public void playerListingChanged(PlayerListing listing)
    {
        updatePlayerListing(listing);
        
    }

    public void doneSelectingPlayers(byte[] gameData)
    {
        GameData data = null;
        try
        {
          data  = new GameDataManager().loadGame(new ByteArrayInputStream(gameData));
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
        }

        m_launcher.setGameData(data);
        m_launcher.startClient();
    }
      
  };



    /**
     * @param playerListingMessage
     */
    private void updatePlayerListing(final PlayerListing playerListingMessage)
    {
        checkVersion(playerListingMessage);

        m_launcher.setGameInfo(playerListingMessage.getGameName(), playerListingMessage.getGameVersion().toString());

        Runnable refresh = new Runnable()
        {
          public void run()
          {
            refreshPlayers(playerListingMessage);
          }
        };

        SwingUtilities.invokeLater(refresh);
    }
  

  class PlayerRow
  {
    private JLabel m_playerNameLabel;
    private JLabel m_playerLabel;
    private JComponent m_playerComponent;
    private String m_localPlayerType;

    PlayerRow(String playerName, String localPlayerType)
    {
      m_playerNameLabel = new JLabel(playerName);
      m_playerLabel = new JLabel("");
      m_playerComponent = new JLabel("");
      m_localPlayerType = localPlayerType;
    }

    public JLabel getName()
    {
      return m_playerNameLabel;
    }

    public JLabel getPlayer()
    {
      return m_playerLabel;
    }

    public String getPlayerName()
    {
      return m_playerNameLabel.getText();
    }

    public void setPlayerName(String playerName)
    {
      if(playerName == null)
      {
        m_playerLabel.setText("-");
        JButton button = new JButton(m_takeAction);
        button.setMargin(BUTTON_INSETS);
        m_playerComponent = button;

      }
      else
      {
        m_playerLabel.setText(playerName);

        if(playerName.equals(m_channelMessenger.getLocalNode().getName()))
        {
          JButton button = new JButton(m_dontTakeAction);
          button.setMargin(BUTTON_INSETS);
          m_playerComponent = button;
        }
        else
        {
          m_playerComponent = new JLabel("");
        }
      }
    }

    public boolean isPlaying()
    {
      return m_playerLabel.getText().equals(m_channelMessenger.getLocalNode().getName());
    }

    public JComponent getPlayerComponent()
    {
      return m_playerComponent;
    }

    public String getLocalType()
    {
      return m_localPlayerType;
    }

    private Action m_takeAction = new AbstractAction("Play")
    {
      public void actionPerformed(ActionEvent e)
      {
          getRemoteServer().takePlayer(m_channelMessenger.getLocalNode(), m_playerNameLabel.getText());
      }
    };

    private Action m_dontTakeAction = new AbstractAction("Dont Play")
    {
      public void actionPerformed(ActionEvent e)
      {
          getRemoteServer().releasePlayer(m_channelMessenger.getLocalNode(), m_playerNameLabel.getText());
      }
    };
  }
  
  
}
