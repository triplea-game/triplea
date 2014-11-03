package games.strategy.engine.framework.headlessGameServer;

import games.strategy.common.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.pbem.PBEMMessagePoster;

import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;

/**
 * Server setup model.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
public class HeadlessServerSetup implements IRemoteModelListener, ISetupPanel
{
	private static final long serialVersionUID = 9021977178348892504L;
	private final List<Observer> m_listeners = new CopyOnWriteArrayList<Observer>();
	private final ServerModel m_model;
	private final GameSelectorModel m_gameSelectorModel;
	private final InGameLobbyWatcherWrapper m_lobbyWatcher = new InGameLobbyWatcherWrapper();
	
	public HeadlessServerSetup(final ServerModel model, final GameSelectorModel gameSelectorModel)
	{
		m_model = model;
		m_gameSelectorModel = gameSelectorModel;
		m_model.setRemoteModelListener(this);
		createLobbyWatcher();
		setupListeners();
		setWidgetActivation();
		internalPlayerListChanged();
	}
	
	public void createLobbyWatcher()
	{
		if (m_lobbyWatcher != null)
		{
			m_lobbyWatcher.setInGameLobbyWatcher(InGameLobbyWatcher.newInGameLobbyWatcher(m_model.getMessenger(), null, m_lobbyWatcher.getInGameLobbyWatcher()));
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
			Thread.sleep(3000);
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
	
	private void setupListeners()
	{
	}
	
	public void setWidgetActivation()
	{
	}
	
	public void shutDown()
	{
		m_model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
		m_model.shutDown();
		if (m_lobbyWatcher != null)
		{
			m_lobbyWatcher.shutDown();
		}
	}
	
	public void cancel()
	{
		m_model.setRemoteModelListener(IRemoteModelListener.NULL_LISTENER);
		m_model.cancel();
		if (m_lobbyWatcher != null)
		{
			m_lobbyWatcher.shutDown();
		}
	}
	
	public boolean canGameStart()
	{
		if (m_gameSelectorModel.getGameData() == null || m_model == null)
			return false;
		final Map<String, String> players = m_model.getPlayersToNodeListing();
		if (players == null || players.isEmpty())
			return false;
		for (final String player : players.keySet())
		{
			if (players.get(player) == null)
				return false;
		}
		// make sure at least 1 player is enabled
		final Map<String, Boolean> someoneEnabled = m_model.getPlayersEnabledListing();
		for (final Boolean bool : someoneEnabled.values())
		{
			if (bool)
				return true;
		}
		return false;
	}
	
	public void playerListChanged()
	{
		internalPlayerListChanged();
	}
	
	public void playersTakenChanged()
	{
		internalPlayersTakenChanged();
	}
	
	private void internalPlayersTakenChanged()
	{
		notifyObservers();
	}
	
	private void internalPlayerListChanged()
	{
		internalPlayersTakenChanged();
	}
	
	public IChatPanel getChatPanel()
	{
		return m_model.getChatPanel();
	}
	
	public ServerModel getModel()
	{
		return m_model;
	}
	
	public synchronized ILauncher getLauncher()
	{
		final ServerLauncher launcher = (ServerLauncher) m_model.getLauncher();
		if (launcher == null)
			return null;
		launcher.setInGameLobbyWatcher(m_lobbyWatcher);
		return launcher;
	}
	
	public List<Action> getUserActions()
	{
		return null;
	}
	
	public void addObserver(final Observer observer)
	{
		m_listeners.add(observer);
	}
	
	public void removeObserver(final Observer observer)
	{
		m_listeners.add(observer);
	}
	
	public void notifyObservers()
	{
		for (final Observer observer : m_listeners)
		{
			observer.update(null, null);
		}
	}
	
	public void preStartGame()
	{
	}
	
	public void postStartGame()
	{
		final GameData data = m_gameSelectorModel.getGameData();
		data.getProperties().set(PBEMMessagePoster.PBEM_GAME_PROP_NAME, false);
	}
}
