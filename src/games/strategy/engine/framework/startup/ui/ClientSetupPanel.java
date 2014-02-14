package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.ui.SaveGameFileChooser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ClientSetupPanel extends SetupPanel
{
	private static final long serialVersionUID = 6942605803526295372L;
	private final Insets BUTTON_INSETS = new Insets(0, 0, 0, 0);
	private final ClientModel m_model;
	private List<PlayerRow> m_playerRows = Collections.emptyList();
	private final IRemoteModelListener m_listener = new IRemoteModelListener()
	{
		public void playersTakenChanged()
		{
		}
		
		public void playerListChanged()
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					internalPlayersChanged();
				}
			});
		}
	};
	
	public ClientSetupPanel(final ClientModel model)
	{
		m_model = model;
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
	}
	
	private void internalPlayersChanged()
	{
		final Map<String, String> players = m_model.getPlayerToNodesMapping();
		final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder = m_model.getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap();
		final Map<String, Boolean> enabledPlayers = m_model.getPlayersEnabledListing();
		final Collection<String> disableable = m_model.getPlayersAllowedToBeDisabled();
		if (!m_model.getIsServerHeadlessCached())
			disableable.clear();// clients only get to change bot settings
		m_playerRows = new ArrayList<PlayerRow>();
		final Set<String> playerNames = playerNamesAndAlliancesInTurnOrder.keySet();
		for (final String name : playerNames)
		{
			final PlayerRow playerRow = new PlayerRow(name, playerNamesAndAlliancesInTurnOrder.get(name), IGameLoader.CLIENT_PLAYER_TYPE, enabledPlayers.get(name));
			m_playerRows.add(playerRow);
			playerRow.update(players.get(name), enabledPlayers.get(name), disableable.contains(name));
		}
		layoutComponents();
	}
	
	private void createComponents()
	{
	}
	
	private void layoutComponents()
	{
		removeAll();
		setLayout(new BorderLayout());
		final JPanel info = new JPanel();
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		info.add(new JLabel(" "));
		add(info, BorderLayout.NORTH);
		final JPanel players = new JPanel();
		final GridBagLayout layout = new GridBagLayout();
		players.setLayout(layout);
		final Insets spacing = new Insets(3, 16, 0, 0);
		final Insets lastSpacing = new Insets(3, 16, 0, 16);
		int gridx = 0;
		final GridBagConstraints enabledPlayerConstraints = new GridBagConstraints();
		final boolean disableable = m_model.getPlayersAllowedToBeDisabled().isEmpty();
		if (!disableable)
		{
			enabledPlayerConstraints.anchor = GridBagConstraints.WEST;
			enabledPlayerConstraints.gridx = gridx++;
			enabledPlayerConstraints.insets = new Insets(3, 20, 0, -10);
		}
		final GridBagConstraints nameConstraints = new GridBagConstraints();
		nameConstraints.anchor = GridBagConstraints.WEST;
		nameConstraints.gridx = gridx++;
		nameConstraints.insets = spacing;
		gridx++;
		final GridBagConstraints playerConstraints = new GridBagConstraints();
		playerConstraints.anchor = GridBagConstraints.WEST;
		playerConstraints.gridx = gridx++;
		playerConstraints.insets = spacing;
		final GridBagConstraints playConstraints = new GridBagConstraints();
		playConstraints.anchor = GridBagConstraints.WEST;
		playConstraints.gridx = gridx++;
		playConstraints.insets = spacing;
		final GridBagConstraints allianceConstraints = new GridBagConstraints();
		allianceConstraints.anchor = GridBagConstraints.WEST;
		allianceConstraints.gridx = gridx++;
		allianceConstraints.insets = lastSpacing;
		if (!disableable)
		{
			final JLabel enableLabel = new JLabel("Use");
			enableLabel.setForeground(Color.black);
			layout.setConstraints(enableLabel, enabledPlayerConstraints);
			players.add(enableLabel);
		}
		final JLabel nameLabel = new JLabel("Name");
		nameLabel.setForeground(Color.black);
		layout.setConstraints(nameLabel, nameConstraints);
		players.add(nameLabel);
		final JLabel playerLabel = new JLabel("Played By");
		playerLabel.setForeground(Color.black);
		layout.setConstraints(playerLabel, playerConstraints);
		players.add(playerLabel);
		final JLabel playedByLabel = new JLabel("                    ");
		layout.setConstraints(playedByLabel, playConstraints);
		players.add(playedByLabel);
		final JLabel allianceLabel = new JLabel("Alliance");
		allianceLabel.setForeground(Color.black);
		layout.setConstraints(allianceLabel, allianceConstraints);
		players.add(allianceLabel);
		for (final PlayerRow row : m_playerRows)
		{
			if (!disableable)
			{
				layout.setConstraints(row.getEnabledPlayer(), enabledPlayerConstraints);
				players.add(row.getEnabledPlayer());
			}
			layout.setConstraints(row.getName(), nameConstraints);
			players.add(row.getName());
			layout.setConstraints(row.getPlayer(), playerConstraints);
			players.add(row.getPlayer());
			layout.setConstraints(row.getPlayerComponent(), playConstraints);
			players.add(row.getPlayerComponent());
			layout.setConstraints(row.getAlliance(), allianceConstraints);
			players.add(row.getAlliance());
		}
		add(players, BorderLayout.CENTER);
		validate();
	}
	
	private void setupListeners()
	{
		m_model.setRemoteModelListener(m_listener);
	}
	
	@Override
	public void setWidgetActivation()
	{
	}
	
	public void shutDown()
	{
		m_model.shutDown();
	}
	
	@Override
	public void cancel()
	{
		m_model.cancel();
	}
	
	@Override
	public boolean canGameStart()
	{
		// our server must handle this
		return false;
	}
	
	
	class PlayerRow
	{
		private final JCheckBox m_enabledCheckBox;
		private final JLabel m_playerNameLabel;
		private final JLabel m_playerLabel;
		private JComponent m_playerComponent;
		private final String m_localPlayerType;
		private JLabel m_alliance;
		
		PlayerRow(final String playerName, final Collection<String> playerAlliances, final String localPlayerType, final boolean enabled)
		{
			m_playerNameLabel = new JLabel(playerName);
			m_playerLabel = new JLabel("");
			m_playerComponent = new JLabel("");
			m_localPlayerType = localPlayerType;
			m_enabledCheckBox = new JCheckBox();
			m_enabledCheckBox.addActionListener(m_disablePlayerActionListener);
			m_enabledCheckBox.setSelected(enabled);
			if (playerAlliances.contains(playerName))
				m_alliance = new JLabel();
			else
				m_alliance = new JLabel(playerAlliances.toString());
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
		
		public JLabel getAlliance()
		{
			return m_alliance;
		}
		
		public JCheckBox getEnabledPlayer()
		{
			return m_enabledCheckBox;
		}
		
		public void update(final String playerName, final boolean enabled, final boolean disableable)
		{
			if (playerName == null)
			{
				m_playerLabel.setText("-");
				final JButton button = new JButton(m_takeAction);
				button.setMargin(BUTTON_INSETS);
				m_playerComponent = button;
			}
			else
			{
				m_playerLabel.setText(playerName);
				if (playerName.equals(m_model.getMessenger().getLocalNode().getName()))
				{
					final JButton button = new JButton(m_dontTakeAction);
					button.setMargin(BUTTON_INSETS);
					m_playerComponent = button;
				}
				else
				{
					m_playerComponent = new JLabel("");
				}
			}
			setWidgetActivation(disableable);
		}
		
		private void setWidgetActivation(final boolean disableable)
		{
			m_playerNameLabel.setEnabled(m_enabledCheckBox.isSelected());
			m_playerLabel.setEnabled(m_enabledCheckBox.isSelected());
			m_playerComponent.setEnabled(m_enabledCheckBox.isSelected());
			m_alliance.setEnabled(m_enabledCheckBox.isSelected());
			m_enabledCheckBox.setEnabled(disableable);
		}
		
		public boolean isPlaying()
		{
			return m_playerLabel.getText().equals(m_model.getMessenger().getLocalNode().getName());
		}
		
		public JComponent getPlayerComponent()
		{
			return m_playerComponent;
		}
		
		public String getLocalType()
		{
			return m_localPlayerType;
		}
		
		private final Action m_takeAction = new AbstractAction("Play")
		{
			private static final long serialVersionUID = 9086754428763609790L;
			
			public void actionPerformed(final ActionEvent e)
			{
				m_model.takePlayer(m_playerNameLabel.getText());
			}
		};
		private final Action m_dontTakeAction = new AbstractAction("Dont Play")
		{
			private static final long serialVersionUID = 8735891444454338978L;
			
			public void actionPerformed(final ActionEvent e)
			{
				m_model.releasePlayer(m_playerNameLabel.getText());
			}
		};
		private final ActionListener m_disablePlayerActionListener = new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (m_enabledCheckBox.isSelected())
					m_model.enablePlayer(m_playerNameLabel.getText());
				else
					m_model.disablePlayer(m_playerNameLabel.getText());
				setWidgetActivation(true);
			}
		};
	}
	
	@Override
	public IChatPanel getChatPanel()
	{
		return m_model.getChatPanel();
	}
	
	@Override
	public List<Action> getUserActions()
	{
		if (m_model == null)
			return null;
		final boolean isServerHeadless = m_model.getIsServerHeadlessCached();
		if (!isServerHeadless)
			return null;
		final List<Action> rVal = new ArrayList<Action>();
		rVal.add(m_model.getHostBotSetMapClientAction(this));
		rVal.add(m_model.getHostBotChangeGameOptionsClientAction(this));
		rVal.add(m_model.getHostBotChangeGameToSaveGameClientAction(this));
		rVal.add(m_model.getHostBotChangeToAutosaveClientAction(this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE));
		rVal.add(m_model.getHostBotChangeToAutosaveClientAction(this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE2));
		rVal.add(m_model.getHostBotChangeToAutosaveClientAction(this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_ODD));
		rVal.add(m_model.getHostBotChangeToAutosaveClientAction(this, SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_EVEN));
		rVal.add(m_model.getHostBotGetGameSaveClientAction(this));
		return rVal;
	}
}
