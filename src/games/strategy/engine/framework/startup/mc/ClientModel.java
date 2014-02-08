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
package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.networkMaintenance.ChangeGameOptionsClientAction;
import games.strategy.engine.framework.networkMaintenance.ChangeGameToSaveGameClientAction;
import games.strategy.engine.framework.networkMaintenance.ChangeToAutosaveClientAction;
import games.strategy.engine.framework.networkMaintenance.GetGameSaveClientAction;
import games.strategy.engine.framework.networkMaintenance.SetMapClientAction;
import games.strategy.engine.framework.startup.launcher.IServerReady;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.ui.ClientOptions;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.framework.ui.background.WaitWindow;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.MacFinder;
import games.strategy.net.Messengers;
import games.strategy.ui.Util;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ClientModel implements IMessengerErrorListener
{
	public static final RemoteName CLIENT_READY_CHANNEL = new RemoteName("games.strategy.engine.framework.startup.mc.ClientModel.CLIENT_READY_CHANNEL", IServerReady.class);
	private static Logger s_logger = Logger.getLogger(ClientModel.class.getName());
	private IRemoteModelListener m_listener = IRemoteModelListener.NULL_LISTENER;
	private IChannelMessenger m_channelMessenger;
	private IRemoteMessenger m_remoteMessenger;
	private IClientMessenger m_messenger;
	private final GameObjectStreamFactory m_objectStreamFactory = new GameObjectStreamFactory(null);
	private final GameSelectorModel m_gameSelectorModel;
	private final SetupPanelModel m_typePanelModel;
	private Component m_ui;
	private IChatPanel m_chatPanel;
	private ClientGame m_game;
	private boolean m_hostIsHeadlessBot = false;
	private final WaitWindow m_gameLoadingWindow = new WaitWindow("Loading game, please wait.");
	// we set the game data to be null, since we
	// are a client game, and the game data lives on the server
	// however, if we cancel, we want to restore the old game data.
	private GameData m_gameDataOnStartup;
	private Map<String, String> m_playersToNodes = new HashMap<String, String>();
	private Map<String, Boolean> m_playersEnabledListing = new HashMap<String, Boolean>();
	private Collection<String> m_playersAllowedToBeDisabled = new HashSet<String>();
	private Map<String, Collection<String>> m_playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<String, Collection<String>>();
	
	ClientModel(final GameSelectorModel gameSelectorModel, final SetupPanelModel typePanelModel)
	{
		m_typePanelModel = typePanelModel;
		m_gameSelectorModel = gameSelectorModel;
	}
	
	public void setRemoteModelListener(IRemoteModelListener listener)
	{
		if (listener == null)
			listener = IRemoteModelListener.NULL_LISTENER;
		m_listener = listener;
	}
	
	private ClientProps getProps(final Component ui)
	{
		if (System.getProperties().getProperty(GameRunner2.TRIPLEA_CLIENT_PROPERTY, "false").equals("true") && System.getProperties().getProperty(GameRunner2.TRIPLEA_STARTED, "").equals(""))
		{
			final ClientProps props = new ClientProps();
			props.setHost(System.getProperty(GameRunner2.TRIPLEA_HOST_PROPERTY));
			props.setName(System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY));
			props.setPort(Integer.parseInt(System.getProperty(GameRunner2.TRIPLEA_PORT_PROPERTY)));
			System.setProperty(GameRunner2.TRIPLEA_STARTED, "true");
			return props;
		}
		// load in the saved name!
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		final String playername = prefs.get(ServerModel.PLAYERNAME, System.getProperty("user.name"));
		final ClientOptions options = new ClientOptions(ui, playername, GameRunner2.PORT, "127.0.0.1");
		options.setLocationRelativeTo(ui);
		options.setVisible(true);
		options.dispose();
		if (!options.getOKPressed())
		{
			return null;
		}
		final ClientProps props = new ClientProps();
		props.setHost(options.getAddress());
		props.setName(options.getName());
		props.setPort(options.getPort());
		return props;
	}
	
	public boolean createClientMessenger(Component ui)
	{
		m_gameDataOnStartup = m_gameSelectorModel.getGameData();
		m_gameSelectorModel.setCanSelect(false);
		ui = JOptionPane.getFrameForComponent(ui);
		m_ui = ui;
		// load in the saved name!
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		final ClientProps props = getProps(ui);
		if (props == null)
		{
			m_gameSelectorModel.setCanSelect(true);
			cancel();
			return false;
		}
		final String name = props.getName();
		s_logger.log(Level.FINE, "Client playing as:" + name);
		// save the name! -- lnxduk
		prefs.put(ServerModel.PLAYERNAME, name);
		final int port = props.getPort();
		if (port >= 65536 || port <= 0)
		{
			EventThreadJOptionPane.showMessageDialog(ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE, new CountDownLatchHandler(true));
			return false;
		}
		final String address = props.getHost();
		try
		{
			final String mac = MacFinder.GetHashedMacAddress();
			m_messenger = new ClientMessenger(address, port, name, mac, m_objectStreamFactory, new ClientLogin(m_ui));
		} catch (final CouldNotLogInException ioe)
		{
			// an error message should have already been reported
			return false;
		} catch (final Exception ioe)
		{
			ioe.printStackTrace(System.out);
			EventThreadJOptionPane.showMessageDialog(ui, "Unable to connect:" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE, new CountDownLatchHandler(true));
			return false;
		}
		m_messenger.addErrorListener(this);
		final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(m_messenger);
		m_channelMessenger = new ChannelMessenger(unifiedMessenger);
		m_remoteMessenger = new RemoteMessenger(unifiedMessenger);
		m_channelMessenger.registerChannelSubscriber(m_channelListener, IClientChannel.CHANNEL_NAME);
		m_chatPanel = new ChatPanel(m_messenger, m_channelMessenger, m_remoteMessenger, ServerModel.CHAT_NAME, Chat.CHAT_SOUND_PROFILE.GAME_CHATROOM);
		if (getIsServerHeadlessTest())
		{
			m_gameSelectorModel.setClientModelForHostBots(this);
			((ChatPanel) m_chatPanel).getChatMessagePanel().addServerMessage("Welcome to an automated dedicated host service (a host bot). "
						+ "\nIf anyone disconnects, the autosave will be reloaded (a save might be loaded right now). "
						+ "\nYou can get the current save, or you can load a save (only saves that it has the map for).");
		}
		m_remoteMessenger.registerRemote(m_observerWaitingToJoin, ServerModel.getObserverWaitingToStartName(m_messenger.getLocalNode()));
		// save this, it will be cleared later
		m_gameDataOnStartup = m_gameSelectorModel.getGameData();
		final IServerStartupRemote serverStartup = getServerStartup();
		final PlayerListing players = serverStartup.getPlayerListing();
		internalePlayerListingChanged(players);
		if (!serverStartup.isGameStarted(m_messenger.getLocalNode()))
		{
			m_remoteMessenger.unregisterRemote(ServerModel.getObserverWaitingToStartName(m_messenger.getLocalNode()));
		}
		m_gameSelectorModel.setIsHostHeadlessBot(m_hostIsHeadlessBot);
		return true;
	}
	
	private IServerStartupRemote getServerStartup()
	{
		return (IServerStartupRemote) m_remoteMessenger.getRemote(ServerModel.SERVER_REMOTE_NAME);
	}
	
	public List<String> getAvailableServerGames()
	{
		final Set<String> games = getServerStartup().getAvailableGames();
		if (games == null)
			return new ArrayList<String>();
		return new ArrayList<String>(games);
	}
	
	public void shutDown()
	{
		if (m_messenger == null)
			return;
		m_objectStreamFactory.setData(null);
		m_messenger.shutDown();
		m_chatPanel.shutDown();
		m_gameSelectorModel.setGameData(null);
		m_gameSelectorModel.setCanSelect(false);
		m_hostIsHeadlessBot = false;
		m_gameSelectorModel.setIsHostHeadlessBot(false);
		m_gameSelectorModel.setClientModelForHostBots(null);
		m_messenger.removeErrorListener(this);
	}
	
	public void cancel()
	{
		if (m_messenger == null)
			return;
		m_objectStreamFactory.setData(null);
		m_messenger.shutDown();
		m_chatPanel.setChat(null);
		m_gameSelectorModel.setGameData(m_gameDataOnStartup);
		m_gameSelectorModel.setCanSelect(true);
		m_hostIsHeadlessBot = false;
		m_gameSelectorModel.setIsHostHeadlessBot(false);
		m_gameSelectorModel.setClientModelForHostBots(null);
		m_messenger.removeErrorListener(this);
	}
	
	private final IClientChannel m_channelListener = new IClientChannel()
	{
		public void playerListingChanged(final PlayerListing listing)
		{
			internalePlayerListingChanged(listing);
		}
		
		public void gameReset()
		{
			m_objectStreamFactory.setData(null);
			Util.runInSwingEventThread(new Runnable()
			{
				public void run()
				{
					MainFrame.getInstance().setVisible(true);
				}
			});
		}
		
		public void doneSelectingPlayers(final byte[] gameData, final Map<String, INode> players)
		{
			final CountDownLatch latch = new CountDownLatch(1);
			startGame(gameData, players, latch, false);
			try
			{
				latch.await(10, TimeUnit.SECONDS);
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	};
	IObserverWaitingToJoin m_observerWaitingToJoin = new IObserverWaitingToJoin()
	{
		public void joinGame(final byte[] gameData, final Map<String, INode> players)
		{
			m_remoteMessenger.unregisterRemote(ServerModel.getObserverWaitingToStartName(m_messenger.getLocalNode()));
			final CountDownLatch latch = new CountDownLatch(1);
			startGame(gameData, players, latch, true);
			try
			{
				latch.await(10, TimeUnit.SECONDS);
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		public void cannotJoinGame(final String reason)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					m_typePanelModel.showSelectType();
					EventThreadJOptionPane.showMessageDialog(m_ui, "Could not join game:" + reason, new CountDownLatchHandler(true));
				}
			});
		}
	};
	
	private void startGame(final byte[] gameData, final Map<String, INode> players, final CountDownLatch onDone, final boolean gameRunning)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_gameLoadingWindow.setVisible(true);
				m_gameLoadingWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(m_ui));
				m_gameLoadingWindow.showWait();
			}
		});
		final Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					startGameInNewThread(gameData, players, gameRunning);
				} catch (final RuntimeException e)
				{
					m_gameLoadingWindow.doneWait();
					throw e;
				} finally
				{
					if (onDone != null)
						onDone.countDown();
				}
			}
		};
		final Thread t = new Thread(r);
		t.start();
	}
	
	private void startGameInNewThread(final byte[] gameData, final Map<String, INode> players, final boolean gameRunning)
	{
		final GameData data;
		try
		{
			data = new GameDataManager().loadGame(new ByteArrayInputStream(gameData), null);
		} catch (final IOException ex)
		{
			ex.printStackTrace();
			return;
		}
		m_objectStreamFactory.setData(data);
		final Map<String, String> playerMapping = new HashMap<String, String>();
		for (final String player : m_playersToNodes.keySet())
		{
			final String playedBy = m_playersToNodes.get(player);
			if (playedBy.equals(m_messenger.getLocalNode().getName()))
			{
				playerMapping.put(player, IGameLoader.CLIENT_PLAYER_TYPE);
			}
		}
		final Set<IGamePlayer> playerSet = data.getGameLoader().createPlayers(playerMapping);
		final Messengers messengers = new Messengers(m_messenger, m_remoteMessenger, m_channelMessenger);
		m_game = new ClientGame(data, playerSet, players, messengers);
		final Thread t = new Thread("Client Game Launcher")
		{
			@Override
			public void run()
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						JOptionPane.getFrameForComponent(m_ui).setVisible(false);
					}
				});
				try
				{
					// game will be null if we loose the connection
					if (m_game != null)
					{
						try
						{
							data.getGameLoader().startGame(m_game, playerSet, false);
							data.testLocksOnRead();
							NewGameChooser.clearNewGameChooserModel();
						} catch (final Exception e)
						{
							e.printStackTrace();
							m_messenger.shutDown();
							m_gameLoadingWindow.doneWait();
							// an ugly hack, we need a better
							// way to get the main frame
							MainFrame.getInstance().clientLeftGame();
						}
					}
					if (!gameRunning)
						((IServerReady) m_remoteMessenger.getRemote(CLIENT_READY_CHANNEL)).clientReady();
				} finally
				{
					m_gameLoadingWindow.doneWait();
				}
			}
		};
		t.start();
	}
	
	public void takePlayer(final String playerName)
	{
		getServerStartup().takePlayer(m_messenger.getLocalNode(), playerName);
	}
	
	public void releasePlayer(final String playerName)
	{
		getServerStartup().releasePlayer(m_messenger.getLocalNode(), playerName);
	}
	
	public void disablePlayer(final String playerName)
	{
		getServerStartup().disablePlayer(playerName);
	}
	
	public void enablePlayer(final String playerName)
	{
		getServerStartup().enablePlayer(playerName);
	}
	
	private void internalePlayerListingChanged(final PlayerListing listing)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_gameSelectorModel.clearDataButKeepGameInfo(listing.getGameName(), listing.getGameRound(), listing.getGameVersion().toString());
			}
		});
		synchronized (this)
		{
			m_playersToNodes = listing.getPlayerToNodeListing();
			m_playersEnabledListing = listing.getPlayersEnabledListing();
			m_playersAllowedToBeDisabled = listing.getPlayersAllowedToBeDisabled();
			m_playerNamesAndAlliancesInTurnOrder = listing.getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap();
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_listener.playerListChanged();
			}
		});
	}
	
	public Map<String, String> getPlayerToNodesMapping()
	{
		synchronized (this)
		{
			return new HashMap<String, String>(m_playersToNodes);
		}
	}
	
	public Map<String, Boolean> getPlayersEnabledListing()
	{
		synchronized (this)
		{
			return new HashMap<String, Boolean>(m_playersEnabledListing);
		}
	}
	
	public Collection<String> getPlayersAllowedToBeDisabled()
	{
		synchronized (this)
		{
			return new HashSet<String>(m_playersAllowedToBeDisabled);
		}
	}
	
	public Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap()
	{
		synchronized (this)
		{
			return new LinkedHashMap<String, Collection<String>>(m_playerNamesAndAlliancesInTurnOrder);
		}
	}
	
	public IClientMessenger getMessenger()
	{
		return m_messenger;
	}
	
	public IServerStartupRemote getServerStartupRemote()
	{
		return getServerStartup();
	}
	
	private void connectionLost()
	{
		EventThreadJOptionPane.showMessageDialog(m_ui, " Connection To Server Lost", "Connection Lost", JOptionPane.ERROR_MESSAGE, new CountDownLatchHandler(true));
		if (m_game != null)
		{
			m_game.shutDown();
			m_game = null;
		}
		MainFrame.getInstance().clientLeftGame();
	}
	
	public void messengerInvalid(final IMessenger messenger, final Exception reason)
	{
		connectionLost();
	}
	
	public IChatPanel getChatPanel()
	{
		return m_chatPanel;
	}
	
	public boolean getIsServerHeadlessTest()
	{
		final IServerStartupRemote serverRemote = getServerStartup();
		if (serverRemote != null)
			m_hostIsHeadlessBot = serverRemote.getIsServerHeadless();
		else
			m_hostIsHeadlessBot = false;
		return m_hostIsHeadlessBot;
	}
	
	public boolean getIsServerHeadlessCached()
	{
		return m_hostIsHeadlessBot;
	}
	
	public Action getHostBotSetMapClientAction(final Component parent)
	{
		return new SetMapClientAction(parent, getMessenger(), getAvailableServerGames());
	}
	
	public Action getHostBotChangeGameOptionsClientAction(final Component parent)
	{
		return new ChangeGameOptionsClientAction(parent, getServerStartupRemote());
	}
	
	public Action getHostBotChangeGameToSaveGameClientAction(final Component parent)
	{
		return new ChangeGameToSaveGameClientAction(parent, getMessenger());
	}
	
	public Action getHostBotChangeToAutosaveClientAction(final Component parent, final SaveGameFileChooser.AUTOSAVE_TYPE autosaveType)
	{
		return new ChangeToAutosaveClientAction(parent, getMessenger(), autosaveType);
	}
	
	public Action getHostBotGetGameSaveClientAction(final Component parent)
	{
		return new GetGameSaveClientAction(parent, getServerStartupRemote());
	}
}


class ClientProps
{
	private int port;
	private String name;
	private String host;
	
	public String getHost()
	{
		return host;
	}
	
	public void setHost(final String host)
	{
		this.host = host;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(final String name)
	{
		this.name = name;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public void setPort(final int port)
	{
		this.port = port;
	}
}
