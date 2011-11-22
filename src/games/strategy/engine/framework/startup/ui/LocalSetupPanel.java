package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class LocalSetupPanel extends SetupPanel implements Observer
{
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
		final Map<String, String> reloadSelections = PlayerID.currentPlayers(data);
		removeAll();
		m_playerTypes.clear();
		setLayout(new GridBagLayout());
		if (data == null)
		{
			add(new JLabel("No game selected!"));
			return;
		}
		final String[] playerTypes = data.getGameLoader().getServerPlayerTypes();
		final String[] playerNames = data.getPlayerList().getNames();
		// if the xml was created correctly, this list will be in turn order. we want to keep it that way.
		// Arrays.sort(playerNames); // alphabetical order
		for (int i = 0; i < playerNames.length; i++)
		{
			final LocalPlayerComboBoxSelector selector = new LocalPlayerComboBoxSelector(playerNames[i], reloadSelections, data.getAllianceTracker().getAlliancesPlayerIsIn(
						data.getPlayerList().getPlayerID(playerNames[i])), playerTypes);
			m_playerTypes.add(selector);
			selector.layout(i, this);
		}
		validate();
		invalidate();
	}
	
	private void setupListeners()
	{
		m_gameSelectorModel.addObserver(this);
	}
	
	private void setWidgetActivation()
	{
	}
	
	@Override
	public boolean canGameStart()
	{
		return m_gameSelectorModel.getGameData() != null;
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
		final Iterator<LocalPlayerComboBoxSelector> iter = m_playerTypes.iterator();
		while (iter.hasNext())
		{
			final LocalPlayerComboBoxSelector item = iter.next();
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
		for (final LocalPlayerComboBoxSelector player : m_playerTypes)
		{
			playerTypes.put(player.getPlayerName(), player.getPlayerType());
		}
		final LocalLauncher launcher = new LocalLauncher(m_gameSelectorModel, randomSource, playerTypes);
		return launcher;
	}
}


class LocalPlayerComboBoxSelector
{
	private final String m_playerName;
	private final JComboBox m_playerTypes;
	private final String m_playerAlliances;
	
	LocalPlayerComboBoxSelector(final String playerName, final Map<String, String> reloadSelections, final Collection<String> playerAlliances, final String[] types)
	{
		m_playerName = playerName;
		m_playerTypes = new JComboBox(types);
		String previousSelection = reloadSelections.get(playerName);
		if (previousSelection.equalsIgnoreCase("Client"))
			previousSelection = "Human";
		if (!(previousSelection.equals("no_one")) && Arrays.asList(types).contains(previousSelection))
		{
			m_playerTypes.setSelectedItem(previousSelection);
		}
		else if (m_playerName.startsWith("Neutral") || playerName.startsWith("AI"))
		{
			m_playerTypes.setSelectedItem("Moore N. Able (AI)");
			// Uncomment to disallow players from changing the default
			// m_playerTypes.setEnabled(false);
		}
		if (playerAlliances.contains(playerName))
			m_playerAlliances = "";
		else
			m_playerAlliances = playerAlliances.toString();
	}
	
	public void layout(final int row, final Container container)
	{
		container.add(new JLabel(m_playerName + ":"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));
		container.add(m_playerTypes, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0));
		container.add(new JLabel(m_playerAlliances.toString()), new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 2, 5, 5), 0, 0));
	}
	
	public String getPlayerName()
	{
		return m_playerName;
	}
	
	public String getPlayerType()
	{
		return (String) m_playerTypes.getSelectedItem();
	}
}
