/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.framework.startup.ui;

import games.strategy.common.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.HeadlessGameServer;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.networkMaintenance.BanPlayerAction;
import games.strategy.engine.framework.networkMaintenance.BootPlayerAction;
import games.strategy.engine.framework.networkMaintenance.MutePlayerAction;
import games.strategy.engine.framework.networkMaintenance.SetPasswordAction;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.net.IServerMessenger;
import games.strategy.triplea.TripleA;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ServerSetupPanel extends SetupPanel implements IRemoteModelListener
{
	private static final long serialVersionUID = -2849872641665561807L;
	private final ServerModel m_model;
	private JTextField m_portField;
	private JTextField m_addressField;
	private JTextField m_nameField;
	private List<PlayerRow> m_playerRows = new ArrayList<PlayerRow>();
	private final GameSelectorModel m_gameSelectorModel;
	private JPanel m_info;
	private JPanel m_networkPanel;
	private final InGameLobbyWatcherWrapper m_lobbyWatcher = new InGameLobbyWatcherWrapper();
	
	public ServerSetupPanel(final ServerModel model, final GameSelectorModel gameSelectorModel)
	{
		m_model = model;
		m_gameSelectorModel = gameSelectorModel;
		m_model.setRemoteModelListener(this);
		createLobbyWatcher();
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
		internalPlayerListChanged();
	}
	
	public void createLobbyWatcher()
	{
		m_lobbyWatcher.setInGameLobbyWatcher(InGameLobbyWatcher.newInGameLobbyWatcher(m_model.getMessenger(), this, m_lobbyWatcher.getInGameLobbyWatcher()));
		if (m_lobbyWatcher != null)
		{
			m_lobbyWatcher.setGameSelectorModel(m_gameSelectorModel);
		}
	}
	
	public synchronized void repostLobbyWatcher(final IGame iGame)
	{
		if (iGame != null)
			return;
		if (canGameStart())
			return;
		System.out.println("Restarting lobby watcher");
		shutDownLobbyWatcher();
		try
		{
			Thread.sleep(1000);
		} catch (final InterruptedException e)
		{
		}
		HeadlessGameServer.resetLobbyHostOldExtensionProperties();
		createLobbyWatcher();
	}
	
	public void shutDownLobbyWatcher()
	{
		if (m_lobbyWatcher != null)
		{
			m_lobbyWatcher.shutDown();
		}
	}
	
	private void createComponents()
	{
		final IServerMessenger messenger = m_model.getMessenger();
		final Color backGround = new JTextField().getBackground();
		m_portField = new JTextField("" + messenger.getLocalNode().getPort());
		m_portField.setEnabled(true);
		m_portField.setEditable(false);
		m_portField.setBackground(backGround);
		m_portField.setColumns(6);
		m_addressField = new JTextField(messenger.getLocalNode().getAddress().getHostAddress());
		m_addressField.setEnabled(true);
		m_addressField.setEditable(false);
		m_addressField.setBackground(backGround);
		m_addressField.setColumns(20);
		m_nameField = new JTextField(messenger.getLocalNode().getName());
		m_nameField.setEnabled(true);
		m_nameField.setEditable(false);
		m_nameField.setBackground(backGround);
		m_nameField.setColumns(20);
		m_info = new JPanel();
		m_networkPanel = new JPanel();
	}
	
	private void layoutComponents()
	{
		setLayout(new BorderLayout());
		m_info.setLayout(new GridBagLayout());
		m_info.add(new JLabel("Name:"), new GridBagConstraints(0, 0, 1, 1, 0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 10, 0, 5), 0, 0));
		m_info.add(new JLabel("Address:"), new GridBagConstraints(0, 1, 1, 1, 0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 10, 0, 5), 0, 0));
		m_info.add(new JLabel("Port:"), new GridBagConstraints(0, 2, 1, 1, 0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 10, 0, 5), 0, 0));
		m_info.add(m_nameField, new GridBagConstraints(1, 0, 1, 1, 0.5, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 0, 0, 5), 0, 0));
		m_info.add(m_addressField, new GridBagConstraints(1, 1, 1, 1, 0.5, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 0, 0, 5), 0, 0));
		m_info.add(m_portField, new GridBagConstraints(1, 2, 1, 1, 0.5, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 0, 0, 5), 0, 0));
		add(m_info, BorderLayout.NORTH);
	}
	
	private void layoutPlayers()
	{
		final JPanel players = new JPanel();
		final GridBagLayout layout = new GridBagLayout();
		players.setLayout(layout);
		final Insets spacing = new Insets(3, 23, 0, 0);
		final Insets lastSpacing = new Insets(3, 23, 0, 23);
		final GridBagConstraints nameConstraints = new GridBagConstraints();
		nameConstraints.anchor = GridBagConstraints.WEST;
		nameConstraints.gridx = 0;
		nameConstraints.insets = spacing;
		final GridBagConstraints playerConstraints = new GridBagConstraints();
		playerConstraints.anchor = GridBagConstraints.WEST;
		playerConstraints.gridx = 1;
		playerConstraints.insets = spacing;
		final GridBagConstraints localConstraints = new GridBagConstraints();
		localConstraints.anchor = GridBagConstraints.WEST;
		localConstraints.gridx = 2;
		localConstraints.insets = spacing;
		final GridBagConstraints typeConstraints = new GridBagConstraints();
		typeConstraints.anchor = GridBagConstraints.WEST;
		typeConstraints.gridx = 3;
		typeConstraints.insets = spacing;
		final GridBagConstraints allianceConstraints = new GridBagConstraints();
		allianceConstraints.anchor = GridBagConstraints.WEST;
		allianceConstraints.gridx = 4;
		allianceConstraints.insets = lastSpacing;
		final JLabel nameLabel = new JLabel("Name");
		nameLabel.setForeground(Color.black);
		layout.setConstraints(nameLabel, nameConstraints);
		players.add(nameLabel);
		final JLabel playedByLabel = new JLabel("Played by");
		playedByLabel.setForeground(Color.black);
		layout.setConstraints(playedByLabel, playerConstraints);
		players.add(playedByLabel);
		final JLabel localLabel = new JLabel("Local");
		localLabel.setForeground(Color.black);
		layout.setConstraints(localLabel, localConstraints);
		players.add(localLabel);
		final JLabel typeLabel = new JLabel("Type");
		typeLabel.setForeground(Color.black);
		layout.setConstraints(typeLabel, typeConstraints);
		players.add(typeLabel);
		final JLabel allianceLabel = new JLabel("Alliance");
		allianceLabel.setForeground(Color.black);
		layout.setConstraints(allianceLabel, allianceConstraints);
		players.add(allianceLabel);
		final Iterator<PlayerRow> iter = m_playerRows.iterator();
		if (!iter.hasNext())
		{
			final JLabel noPlayers = new JLabel("Load a game file first");
			layout.setConstraints(noPlayers, nameConstraints);
			players.add(noPlayers);
		}
		while (iter.hasNext())
		{
			final PlayerRow row = iter.next();
			layout.setConstraints(row.getName(), nameConstraints);
			players.add(row.getName());
			layout.setConstraints(row.getPlayer(), playerConstraints);
			players.add(row.getPlayer());
			layout.setConstraints(row.getLocal(), localConstraints);
			players.add(row.getLocal());
			layout.setConstraints(row.getType(), typeConstraints);
			players.add(row.getType());
			layout.setConstraints(row.getAlliance(), allianceConstraints);
			players.add(row.getAlliance());
		}
		removeAll();
		add(m_info, BorderLayout.NORTH);
		final JScrollPane scroll = new JScrollPane(players, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setBorder(null);
		scroll.setViewportBorder(null);
		add(scroll, BorderLayout.CENTER);
		add(m_networkPanel, BorderLayout.SOUTH);
		invalidate();
		validate();
	}
	
	private void setupListeners()
	{
	}
	
	@Override
	public void setWidgetActivation()
	{
	}
	
	@Override
	public void cancel()
	{
		m_model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
		m_model.cancel();
		if (m_lobbyWatcher != null)
		{
			m_lobbyWatcher.shutDown();
		}
	}
	
	@Override
	public boolean canGameStart()
	{
		if (m_gameSelectorModel.getGameData() == null || m_model == null)
			return false;
		final Map<String, String> players = m_model.getPlayers();
		if (players == null || players.isEmpty())
			return false;
		for (final String player : players.keySet())
		{
			if (players.get(player) == null)
				return false;
		}
		return true;
	}
	
	public void playerListChanged()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				internalPlayerListChanged();
			}
		});
	}
	
	public void playersTakenChanged()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				internalPlayersTakenChanged();
			}
		});
	}
	
	private void internalPlayersTakenChanged()
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
		final Map<String, String> players = m_model.getPlayers();
		for (final PlayerRow row : m_playerRows)
		{
			row.update(players);
		}
		super.notifyObservers();
	}
	
	private void internalPlayerListChanged()
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("Wrong thread");
		m_playerRows = new ArrayList<PlayerRow>();
		final Map<String, String> players = m_model.getPlayers();
		final Map<String, Collection<String>> m_playerNamesAndAlliancesInTurnOrder = m_model.getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap();
		final Map<String, String> reloadSelections = PlayerID.currentPlayers(m_gameSelectorModel.getGameData());
		// List<String> keys = new ArrayList<String>(players.keySet());
		// Collections.sort(keys);//we don't want to sort them alphabetically. let them stay in turn order.
		final Set<String> playerNames = m_playerNamesAndAlliancesInTurnOrder.keySet();
		for (final String name : playerNames)
		{
			final PlayerRow newPlayerRow = new PlayerRow(name, reloadSelections, m_playerNamesAndAlliancesInTurnOrder.get(name), m_gameSelectorModel.getGameData().getGameLoader()
						.getServerPlayerTypes());
			m_playerRows.add(newPlayerRow);
			newPlayerRow.update(players);
		}
		layoutPlayers();
		internalPlayersTakenChanged();
	}
	
	
	class PlayerRow
	{
		private final JLabel m_nameLabel;
		private final JLabel m_playerLabel;
		private final JCheckBox m_localCheckBox;
		private final JComboBox m_type;
		private JLabel m_alliance;
		
		PlayerRow(final String playerName, final Map<String, String> reloadSelections, final Collection<String> playerAlliances, final String[] types)
		{
			m_nameLabel = new JLabel(playerName);
			m_playerLabel = new JLabel(m_model.getMessenger().getLocalNode().getName());
			m_localCheckBox = new JCheckBox();
			m_localCheckBox.addActionListener(m_actionListener);
			m_localCheckBox.setSelected(true);
			m_type = new JComboBox(types);
			String previousSelection = reloadSelections.get(playerName);
			if (previousSelection.equalsIgnoreCase("Client"))
				previousSelection = TripleA.HUMAN_PLAYER_TYPE;
			if (!(previousSelection.equals("no_one")) && Arrays.asList(types).contains(previousSelection))
			{
				m_type.setSelectedItem(previousSelection);
				m_model.setLocalPlayerType(m_nameLabel.getText(), (String) m_type.getSelectedItem());
			}
			else if (playerName.startsWith("Neutral") || playerName.startsWith("AI"))
			{
				m_type.setSelectedItem(TripleA.STRONG_COMPUTER_PLAYER_TYPE);
				m_model.setLocalPlayerType(m_nameLabel.getText(), (String) m_type.getSelectedItem());
			}
			if (playerAlliances.contains(playerName))
				m_alliance = new JLabel();
			else
				m_alliance = new JLabel(playerAlliances.toString());
			m_type.addActionListener(new ActionListener()
			{
				public void actionPerformed(final ActionEvent e)
				{
					m_model.setLocalPlayerType(m_nameLabel.getText(), (String) m_type.getSelectedItem());
				}
			});
		}
		
		public JComboBox getType()
		{
			return m_type;
		}
		
		public JLabel getName()
		{
			return m_nameLabel;
		}
		
		public JLabel getAlliance()
		{
			return m_alliance;
		}
		
		public JLabel getPlayer()
		{
			return m_playerLabel;
		}
		
		public JCheckBox getLocal()
		{
			return m_localCheckBox;
		}
		
		public void update(final Map<String, String> players)
		{
			String text = players.get(m_nameLabel.getText());
			if (text == null)
				text = "-";
			m_playerLabel.setText(text);
			m_localCheckBox.setSelected(text.equals(m_model.getMessenger().getLocalNode().getName()));
			setWidgetActivation();
		}
		
		private void setWidgetActivation()
		{
			m_type.setEnabled(m_localCheckBox.isSelected());
		}
		
		private final ActionListener m_actionListener = new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (m_localCheckBox.isSelected())
					m_model.takePlayer(m_nameLabel.getText());
				else
					m_model.releasePlayer(m_nameLabel.getText());
				setWidgetActivation();
			}
		};
	}
	
	@Override
	public ChatPanel getChatPanel()
	{
		return m_model.getChatPanel();
	}
	
	public ServerModel getModel()
	{
		return m_model;
	}
	
	@Override
	public synchronized ILauncher getLauncher()
	{
		final ServerLauncher launcher = (ServerLauncher) m_model.getLauncher();
		launcher.setInGameLobbyWatcher(m_lobbyWatcher);
		return launcher;
	}
	
	@Override
	public List<Action> getUserActions()
	{
		final List<Action> rVal = new ArrayList<Action>();
		rVal.add(new BootPlayerAction(this, m_model.getMessenger()));
		rVal.add(new BanPlayerAction(this, m_model.getMessenger()));
		rVal.add(new MutePlayerAction(this, m_model.getMessenger()));
		rVal.add(new SetPasswordAction(this, m_lobbyWatcher, (ClientLoginValidator) m_model.getMessenger().getLoginValidator()));
		if (m_lobbyWatcher != null && m_lobbyWatcher.isActive())
		{
			rVal.add(new EditGameCommentAction(m_lobbyWatcher, ServerSetupPanel.this));
			rVal.add(new RemoveGameFromLobbyAction(m_lobbyWatcher));
		}
		return rVal;
	}
}
