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

import java.io.Serializable;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import games.strategy.engine.data.*;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.message.*;
import games.strategy.net.*; 

import games.strategy.engine.EngineVersion;

/**
 *
 * UI for starting the client.
 *
 * @author  Sean Bridges
 */
public class ClientStartup extends JFrame
{
	private static final Insets BUTTON_INSETS = new Insets(0,0,0,0);
	
	private final Object m_lock = new Object();
	
	private final IGameLoader m_loader;
	private final IMessenger m_messenger;
	private final GameData m_data;
	
	private JLabel m_nameLabel;
	
	//list of PlayerRows
	private ArrayList m_playerRows;
	
	/**
	 * Creates a new instance of ServerStartup
	 */
	public ClientStartup(IGameLoader loader, GameData data, IMessenger messenger)
	{
		super("Client");

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		m_loader = loader;
		m_data = data;
		m_messenger = messenger;
		
		initComponents();
		layoutComponents();
		setWidgetActivation();
		pack();
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
			if(row.isPlaying())
			{
				map.put(row.getPlayerName(), row.getLocalType());
			}
		}
		return map;
	}
		
	public void waitForPlayers()
	{
		m_messenger.addMessageListener(m_messageListener);
		m_messenger.broadcast(new ListPlayerRequest());	
		
		try
		{
			synchronized(m_lock)
			{
				m_lock.wait();
			}
			
		} catch(InterruptedException ie)
		{
			ie.printStackTrace();
		}
		
		
		m_messenger.removeMessageListener(m_messageListener);
	}
	
	private void initComponents()
	{
		m_nameLabel = new JLabel("Name:" + m_messenger.getLocalNode().getName());	
		
		String[] remoteOptions = m_loader.getClientPlayerTypes();
		
		m_playerRows = new ArrayList();
		Iterator iter = m_data.getPlayerList().getPlayers().iterator();
		
		while(iter.hasNext())
		{
			PlayerID id = (PlayerID) iter.next();
			m_playerRows.add(new PlayerRow(id.getName(),remoteOptions[0]));
		}
	}
		
	private void layoutComponents()
	{
		Container content = getContentPane();
		
		content.removeAll();
		content.setLayout(new BorderLayout());

		JPanel info = new JPanel();
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		info.add(m_nameLabel);
		info.add(new JLabel(" "));
		content.add(info, BorderLayout.NORTH);
		
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
		
		Iterator iter = m_playerRows.iterator();
		while(iter.hasNext())
		{
			PlayerRow row = (PlayerRow) iter.next();
			
			layout.setConstraints(row.getName(), nameConstraints);
			players.add(row.getName());
			
			layout.setConstraints(row.getPlayer(), playerConstraints);
			players.add(row.getPlayer());
			
			layout.setConstraints(row.getPlayerComponent(), playConstraints);
			players.add(row.getPlayerComponent());	
		}
		
		content.add(players, BorderLayout.CENTER);
		
		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BorderLayout());
		lowerPanel.add(new JLabel(" "), BorderLayout.NORTH);
		
		JPanel buttons = new JPanel();
		buttons.add(new JButton(m_cancelAction));
		
		lowerPanel.add(buttons, BorderLayout.CENTER);
		
		content.add(lowerPanel, BorderLayout.SOUTH);
	}
	
	private void checkVersion(PlayerListingMessage msg)
	{
		if(!msg.getGameName().equals(m_data.getGameName()) ||
		   !msg.getGameVersion().equals(m_data.getGameVersion()) ||
		   !msg.getEngineVersion().equals(EngineVersion.VERSION)
		   )
		{
			try
			{
				StringBuffer version = new StringBuffer();
				
				version.append("Error, server running different game or version.  Cannot join game. \n");
				
				version.append("Server\n engineVersion:").append(msg.getEngineVersion());
				version.append(" gameName:").append(msg.getGameName());
				version.append(" gameVersion:").append(msg.getGameVersion());
				
				version.append("\nClient\n engineVersion:").append(EngineVersion.VERSION);
				version.append(" gameName:").append(m_data.getGameName());
				version.append(" gameVersion:").append(m_data.getGameVersion());
				
				System.err.println(version.toString());
				
				JOptionPane.showMessageDialog(this, version.toString(), "Error", JOptionPane.ERROR_MESSAGE);
				m_messenger.shutDown();
			} finally
			{
				System.exit(0);
			}
		}
	}
	
	private void setWidgetActivation()
	{
	}
	
	private Action m_cancelAction = new AbstractAction("Quit")
	{
		public void actionPerformed(ActionEvent e)
		{
			System.exit(0);
		}
	};
	
	private IMessageListener m_messageListener = new IMessageListener()
	{
		public void messageReceived(Serializable msg, INode from)
		{
			if(msg instanceof DonePlayerSelectionMessage)
			{
				synchronized(m_lock)
				{
					m_lock.notifyAll();
				}
			}
			if(msg instanceof PlayerListingMessage)
			{
				checkVersion((PlayerListingMessage) msg);
				
				Map listing = ((PlayerListingMessage) msg).getPlayerListing();
				Iterator listings = listing.keySet().iterator();
				
				while(listings.hasNext())
				{
					String playerName = (String) listings.next();
					String node = (String) listing.get(playerName);
					
					Iterator rows = m_playerRows.iterator();
					while(rows.hasNext())
					{
						PlayerRow row =  (PlayerRow) rows.next();
						if(row.getPlayerName().equals(playerName))
						{
							row.setPlayerName(node);
						}
					}
					
				}
				Runnable refresh = new Runnable()
				{
					public void run()
					{
						layoutComponents();
						pack();
					}
				};
				
				SwingUtilities.invokeLater(refresh);
			}
		}	
	};
	
	class PlayerRow
	{
		private JLabel m_nameLabel;
		private JLabel m_playerLabel;
		private JComponent m_playerComponent;
		private String m_localPlayerType;
		
		PlayerRow(String playerName, String localPlayerType)
		{
			m_nameLabel = new JLabel(playerName);
			m_playerLabel = new JLabel("");
			m_playerComponent = new JLabel("");
			m_localPlayerType = localPlayerType;
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
				
				if(playerName.equals(m_messenger.getLocalNode().getName()))
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
			return m_playerLabel.getText().equals(m_messenger.getLocalNode().getName());
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
				TakePlayerMessage msg = new TakePlayerMessage(m_nameLabel.getText(), true);
				//TODO send to server only
				m_messenger.broadcast(msg);
			}
		};	
		
		private Action m_dontTakeAction = new AbstractAction("Dont Play")
		{
			public void actionPerformed(ActionEvent e)
			{
				TakePlayerMessage msg = new TakePlayerMessage(m_nameLabel.getText(), false);
				//TODO send to server only
				m_messenger.broadcast(msg);
			}
		};	
	}
}