package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class LocalSetupPanel extends SetupPanel implements Observer
{
	private static final long serialVersionUID = 2284030734590389060L;
	private final GameSelectorModel m_gameSelectorModel;
	private final List<LocalPlayerComboBoxSelector> m_playerTypes = new ArrayList<LocalPlayerComboBoxSelector>();
	
	public LocalSetupPanel(final GameSelectorModel model)
	{
		m_gameSelectorModel = model;
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
	}
	
	private void createComponents()
	{
	}
	
	private void layoutComponents()
	{
		final GameData data = m_gameSelectorModel.getGameData();
		removeAll();
		m_playerTypes.clear();
		setLayout(new GridBagLayout());
		if (data == null)
		{
			add(new JLabel("No game selected!"));
			return;
		}
		final Collection<String> disableable = data.getPlayerList().getPlayersThatMayBeDisabled();
		final HashMap<String, Boolean> playersEnablementListing = data.getPlayerList().getPlayersEnabledListing();
		final Map<String, String> reloadSelections = PlayerID.currentPlayers(data);
		final String[] playerTypes = data.getGameLoader().getServerPlayerTypes();
		final String[] playerNames = data.getPlayerList().getNames();
		// if the xml was created correctly, this list will be in turn order. we want to keep it that way.
		int gridx = 0;
		int gridy = 0;
		if (!disableable.isEmpty() || playersEnablementListing.containsValue(Boolean.FALSE))
		{
			final JLabel enableLabel = new JLabel("Use");
			enableLabel.setForeground(Color.black);
			this.add(enableLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		}
		final JLabel nameLabel = new JLabel("Name");
		nameLabel.setForeground(Color.black);
		this.add(nameLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		final JLabel typeLabel = new JLabel("Type");
		typeLabel.setForeground(Color.black);
		this.add(typeLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		final JLabel allianceLabel = new JLabel("Alliance");
		allianceLabel.setForeground(Color.black);
		this.add(allianceLabel, new GridBagConstraints(gridx++, gridy, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
		for (int i = 0; i < playerNames.length; i++)
		{
			final LocalPlayerComboBoxSelector selector = new LocalPlayerComboBoxSelector(playerNames[i], reloadSelections, disableable, playersEnablementListing,
						data.getAllianceTracker().getAlliancesPlayerIsIn(data.getPlayerList().getPlayerID(playerNames[i])), playerTypes, this);
			m_playerTypes.add(selector);
			selector.layout(++gridy, this);
		}
		validate();
		invalidate();
		setWidgetActivation();
	}
	
	private void setupListeners()
	{
		m_gameSelectorModel.addObserver(this);
	}
	
	@Override
	public void setWidgetActivation()
	{
	}
	
	@Override
	public boolean canGameStart()
	{
		if (m_gameSelectorModel.getGameData() == null)
			return false;
		// make sure at least 1 player is enabled
		for (final LocalPlayerComboBoxSelector player : m_playerTypes)
		{
			if (player.isPlayerEnabled())
				return true;
		}
		return false;
	}
	
	public void shutDown()
	{
		m_gameSelectorModel.deleteObserver(this);
	}
	
	@Override
	public void cancel()
	{
		m_gameSelectorModel.deleteObserver(this);
	}
	
	public void update(final Observable o, final Object arg)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					layoutComponents();
				}
			});
			return;
		}
		layoutComponents();
	}
	
	public String getPlayerType(final String playerName)
	{
		for (final LocalPlayerComboBoxSelector item : m_playerTypes)
		{
			if (item.getPlayerName().equals(playerName))
				return item.getPlayerType();
		}
		throw new IllegalStateException("No player found:" + playerName);
	}
	
	@Override
	public ILauncher getLauncher()
	{
		final IRandomSource randomSource = new PlainRandomSource();
		final Map<String, String> playerTypes = new HashMap<String, String>();
		final Map<String, Boolean> playersEnabled = new HashMap<String, Boolean>();
		for (final LocalPlayerComboBoxSelector player : m_playerTypes)
		{
			playerTypes.put(player.getPlayerName(), player.getPlayerType());
			playersEnabled.put(player.getPlayerName(), player.isPlayerEnabled());
		}
		final PlayerListing pl = new PlayerListing(null, playersEnabled, playerTypes, m_gameSelectorModel.getGameData().getGameVersion(), m_gameSelectorModel.getGameName(),
					m_gameSelectorModel.getGameRound(), null, null); // we don't need the playerToNode list, the disable-able players, or the alliances list, for a local game
		final LocalLauncher launcher = new LocalLauncher(m_gameSelectorModel, randomSource, pl);
		return launcher;
	}
}


class LocalPlayerComboBoxSelector
{
	private final JCheckBox m_enabledCheckBox;
	private final String m_playerName;
	private final JComboBox m_playerTypes;
	private final String m_playerAlliances;
	private boolean m_enabled = true;
	private final JLabel m_name;
	private final JLabel m_alliances;
	private final Collection<String> m_disableable;
	private final String[] m_types;
	private final SetupPanel m_parent;
	
	LocalPlayerComboBoxSelector(final String playerName, final Map<String, String> reloadSelections, final Collection<String> disableable, final HashMap<String, Boolean> playersEnablementListing,
				final Collection<String> playerAlliances, final String[] types, final SetupPanel parent)
	{
		m_playerName = playerName;
		m_name = new JLabel(m_playerName + ":");
		m_enabledCheckBox = new JCheckBox();
		m_enabledCheckBox.addActionListener(m_disablePlayerActionListener);
		m_enabledCheckBox.setSelected(playersEnablementListing.get(playerName));
		m_enabledCheckBox.setEnabled(disableable.contains(playerName));
		m_disableable = disableable;
		m_parent = parent;
		m_types = types;
		m_playerTypes = new JComboBox(types);
		String previousSelection = reloadSelections.get(playerName);
		if (previousSelection.equalsIgnoreCase("Client"))
			previousSelection = types[0];
		if (!(previousSelection.equals("no_one")) && Arrays.asList(types).contains(previousSelection))
		{
			m_playerTypes.setSelectedItem(previousSelection);
		}
		else if (m_playerName.startsWith("Neutral") || playerName.startsWith("AI"))
		{
			m_playerTypes.setSelectedItem(types[Math.max(0, Math.min(types.length - 1, 2))]); // the 3rd in the list should be Moore N Able
			// Uncomment to disallow players from changing the default
			// m_playerTypes.setEnabled(false);
		}
		if (playerAlliances.contains(playerName))
			m_playerAlliances = "";
		else
			m_playerAlliances = playerAlliances.toString();
		m_alliances = new JLabel(m_playerAlliances.toString());
		setWidgetActivation();
	}
	
	public void layout(final int row, final Container container)
	{
		int gridx = 0;
		if (!m_disableable.isEmpty())
		{
			container.add(m_enabledCheckBox, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		}
		container.add(m_name, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		container.add(m_playerTypes, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
		container.add(m_alliances, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
	}
	
	public String getPlayerName()
	{
		return m_playerName;
	}
	
	public String getPlayerType()
	{
		return (String) m_playerTypes.getSelectedItem();
	}
	
	public boolean isPlayerEnabled()
	{
		return m_enabledCheckBox.isSelected();
	}
	
	private void setWidgetActivation()
	{
		m_name.setEnabled(m_enabled);
		m_alliances.setEnabled(m_enabled);
		m_enabledCheckBox.setEnabled(m_disableable.contains(m_playerName));
		m_parent.notifyObservers();
	}
	
	private final ActionListener m_disablePlayerActionListener = new ActionListener()
	{
		public void actionPerformed(final ActionEvent e)
		{
			if (m_enabledCheckBox.isSelected())
			{
				m_enabled = true;
				m_playerTypes.setSelectedItem(m_types[0]); // the 1st in the list should be human
			}
			else
			{
				m_enabled = false;
				m_playerTypes.setSelectedItem(m_types[Math.max(0, Math.min(m_types.length - 1, 1))]); // the 2nd in the list should be Weak AI
			}
			setWidgetActivation();
		}
	};
}
