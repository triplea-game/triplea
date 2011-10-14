package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class ClientSetupPanel extends SetupPanel
{
	private final Insets BUTTON_INSETS = new Insets(0, 0, 0, 0);
	private final ClientModel m_model;
	
	private List<PlayerRow> m_playerRows = Collections.emptyList();
	
	private IRemoteModelListener m_listener = new IRemoteModelListener()
	{
		@Override
		public void playersTakenChanged()
		{
		}
		
		@Override
		public void playerListChanged()
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					internalPlayersChanged();
				}
			});
			
		}
		
	};
	
	public ClientSetupPanel(ClientModel model)
	{
		m_model = model;
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
	}
	
	private void internalPlayersChanged()
	{
		Map<String, String> m_players = m_model.getPlayerMapping();
		Map<String, Collection<String>> m_playerNamesAndAlliancesInTurnOrder = m_model.getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap();
		
		m_playerRows = new ArrayList<PlayerRow>();
		
		// List<String> keys = new ArrayList<String>(m_players.keySet());
		// Collections.sort(keys);//we don't want to sort them alphabetically. let them stay in turn order.
		
		Set<String> playerNames = m_playerNamesAndAlliancesInTurnOrder.keySet();
		for (String name : playerNames)
		{
			PlayerRow playerRow = new PlayerRow(name,
						m_playerNamesAndAlliancesInTurnOrder.get(name),
						IGameLoader.CLIENT_PLAYER_TYPE);
			m_playerRows.add(playerRow);
			playerRow.setPlayerName(m_players.get(name));
			
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
		
		JPanel info = new JPanel();
		info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
		
		info.add(new JLabel(" "));
		add(info, BorderLayout.NORTH);
		
		JPanel players = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		players.setLayout(layout);
		
		Insets spacing = new Insets(3, 23, 0, 0);
		Insets lastSpacing = new Insets(3, 23, 0, 23);
		
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
		playConstraints.insets = spacing;
		
		GridBagConstraints allianceConstraints = new GridBagConstraints();
		allianceConstraints.anchor = GridBagConstraints.WEST;
		allianceConstraints.gridx = 4;
		allianceConstraints.insets = lastSpacing;
		
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
		
		JLabel allianceLabel = new JLabel("Alliance");
		allianceLabel.setForeground(Color.black);
		layout.setConstraints(allianceLabel, allianceConstraints);
		players.add(allianceLabel);
		
		Iterator<PlayerRow> iter = m_playerRows.iterator();
		while (iter.hasNext())
		{
			PlayerRow row = iter.next();
			
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
	
	private void setWidgetActivation()
	{
		
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
		private JLabel m_playerNameLabel;
		private JLabel m_playerLabel;
		private JComponent m_playerComponent;
		private String m_localPlayerType;
		private JLabel m_alliance;
		
		PlayerRow(String playerName, Collection<String> playerAlliances, String localPlayerType)
		{
			m_playerNameLabel = new JLabel(playerName);
			m_playerLabel = new JLabel("");
			m_playerComponent = new JLabel("");
			m_localPlayerType = localPlayerType;
			
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
		
		public void setPlayerName(String playerName)
		{
			if (playerName == null)
			{
				m_playerLabel.setText("-");
				JButton button = new JButton(m_takeAction);
				button.setMargin(BUTTON_INSETS);
				m_playerComponent = button;
				
			}
			else
			{
				m_playerLabel.setText(playerName);
				
				if (playerName.equals(m_model.getMessenger().getLocalNode().getName()))
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
		
		private Action m_takeAction = new AbstractAction("Play")
		{
			@Override
			public void actionPerformed(ActionEvent e)
		{
			m_model.takePlayer(m_playerNameLabel.getText());
		}
		};
		
		private Action m_dontTakeAction = new AbstractAction("Dont Play")
		{
			@Override
			public void actionPerformed(ActionEvent e)
		{
			m_model.releasePlayer(m_playerNameLabel.getText());
		}
		};
	}
	
	@Override
	public ChatPanel getChatPanel()
	{
		return m_model.getChatPanel();
	}
	
}
