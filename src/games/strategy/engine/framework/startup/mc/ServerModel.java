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
import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.HeadlessChat;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.HeadlessGameServer;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.lobby.server.NullModeratorController;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.UnifiedMessenger;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.IMessengerErrorListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.ServerMessenger;
import games.strategy.util.Version;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

public class ServerModel extends Observable implements IMessengerErrorListener, IConnectionChangeListener
{
	public static final String CHAT_NAME = "games.strategy.engine.framework.ui.ServerStartup.CHAT_NAME";
	public static final RemoteName SERVER_REMOTE_NAME = new RemoteName("games.strategy.engine.framework.ui.ServerStartup.SERVER_REMOTE", IServerStartupRemote.class);
	
	public static RemoteName getObserverWaitingToStartName(final INode node)
	{
		return new RemoteName("games.strategy.engine.framework.startup.mc.ServerModel.OBSERVER" + node.getName(), IObserverWaitingToJoin.class);
	}
	
	final static String PLAYERNAME = "PlayerName";
	private static Logger s_logger = Logger.getLogger(ServerModel.class.getName());
	private final GameObjectStreamFactory m_objectStreamFactory = new GameObjectStreamFactory(null);
	private final SetupPanelModel m_typePanelModel;
	private final boolean m_headless;
	private IServerMessenger m_serverMessenger;
	private IRemoteMessenger m_remoteMessenger;
	private IChannelMessenger m_channelMessenger;
	private GameData m_data;
	private Map<String, String> m_players = new HashMap<String, String>();
	private Map<String, Collection<String>> m_playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<String, Collection<String>>();
	private IRemoteModelListener m_listener = IRemoteModelListener.NULL_LISTENER;
	private final GameSelectorModel m_gameSelectorModel;
	private Component m_ui;
	private IChatPanel m_chatPanel;
	private ChatController m_chatController;
	private final Map<String, String> m_localPlayerTypes = new HashMap<String, String>();
	// while our server launcher is not null, delegate new/lost connections to it
	private volatile ServerLauncher m_serverLauncher;
	private final Observer m_gameSelectorObserver = new Observer()
	{
		public void update(final Observable o, final Object arg)
		{
			gameDataChanged();
		}
	};
	
	public ServerModel(final GameSelectorModel gameSelectorModel, final SetupPanelModel typePanelModel)
	{
		this(gameSelectorModel, typePanelModel, false);
	}
	
	public ServerModel(final GameSelectorModel gameSelectorModel, final SetupPanelModel typePanelModel, final boolean headless)
	{
		m_gameSelectorModel = gameSelectorModel;
		m_typePanelModel = typePanelModel;
		m_gameSelectorModel.addObserver(m_gameSelectorObserver);
		m_headless = headless;
	}
	
	public void shutDown()
	{
		m_gameSelectorModel.deleteObserver(m_gameSelectorObserver);
		if (m_serverMessenger != null)
		{
			m_chatController.deactivate();
			m_serverMessenger.shutDown();
			m_serverMessenger.removeErrorListener(this);
			m_chatPanel.shutDown();
		}
	}
	
	public void cancel()
	{
		m_gameSelectorModel.deleteObserver(m_gameSelectorObserver);
		if (m_serverMessenger != null)
		{
			m_chatController.deactivate();
			m_serverMessenger.shutDown();
			m_serverMessenger.removeErrorListener(this);
			m_chatPanel.setChat(null);
		}
	}
	
	public void setRemoteModelListener(IRemoteModelListener listener)
	{
		if (listener == null)
			listener = IRemoteModelListener.NULL_LISTENER;
		m_listener = listener;
	}
	
	public void setLocalPlayerType(final String player, final String type)
	{
		synchronized (this)
		{
			m_localPlayerTypes.put(player, type);
		}
	}
	
	public String getLocalPlayerType(final String player)
	{
		return m_localPlayerTypes.get(player);
	}
	
	private void gameDataChanged()
	{
		synchronized (this)
		{
			m_data = m_gameSelectorModel.getGameData();
			if (m_data != null)
			{
				m_players = new HashMap<String, String>();
				m_playerNamesAndAlliancesInTurnOrder = new LinkedHashMap<String, Collection<String>>();
				for (final String name : m_data.getPlayerList().getNames())
				{
					if (m_headless)// && !(name.startsWith("Neutral") || name.startsWith("AI"))) // debateable whether we want a headless server doing any AI stuff
						m_players.put(name, null);
					else
						m_players.put(name, m_serverMessenger.getLocalNode().getName());
					m_playerNamesAndAlliancesInTurnOrder.put(name, m_data.getAllianceTracker().getAlliancesPlayerIsIn(m_data.getPlayerList().getPlayerID(name)));
				}
			}
			m_objectStreamFactory.setData(m_data);
			m_localPlayerTypes.clear();
		}
		notifyChanellPlayersChanged();
		m_listener.playerListChanged();
	}
	
	private ServerProps getServerProps(final Component ui)
	{
		if (System.getProperties().getProperty(GameRunner2.TRIPLEA_SERVER_PROPERTY, "false").equals("true") && System.getProperties().getProperty(GameRunner2.TRIPLEA_STARTED, "").equals(""))
		{
			final ServerProps props = new ServerProps();
			props.setName(System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY));
			props.setPort(Integer.parseInt(System.getProperty(GameRunner2.TRIPLEA_PORT_PROPERTY)));
			if (System.getProperty(GameRunner2.TRIPLEA_SERVER_PASSWORD_PROPERTY) != null)
			{
				props.setPassword(System.getProperty(GameRunner2.TRIPLEA_SERVER_PASSWORD_PROPERTY));
			}
			System.setProperty(GameRunner2.TRIPLEA_STARTED, "true");
			return props;
		}
		final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		final String playername = prefs.get(PLAYERNAME, System.getProperty("user.name"));
		final ServerOptions options = new ServerOptions(ui, playername, GameRunner2.PORT, false);
		options.setLocationRelativeTo(ui);
		options.setVisible(true);
		options.dispose();
		if (!options.getOKPressed())
		{
			return null;
		}
		final String name = options.getName();
		s_logger.log(Level.FINE, "Server playing as:" + name);
		// save the name! -- lnxduk
		prefs.put(PLAYERNAME, name);
		final int port = options.getPort();
		if (port >= 65536 || port == 0)
		{
			if (m_headless)
				System.out.println("Invalid Port: " + port);
			else
				JOptionPane.showMessageDialog(ui, "Invalid Port: " + port, "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}
		final ServerProps props = new ServerProps();
		props.setName(options.getName());
		props.setPort(options.getPort());
		props.setPassword(options.getPassword());
		return props;
	}
	
	/**
	 * UI can be null. We use it as the parent for message dialogs we show.
	 * If you have a component displayed, use it.
	 */
	public boolean createServerMessenger(Component ui)
	{
		ui = ui == null ? null : JOptionPane.getFrameForComponent(ui);
		m_ui = ui;
		final ServerProps props = getServerProps(ui);
		if (props == null)
			return false;
		try
		{
			m_serverMessenger = new ServerMessenger(props.getName(), props.getPort(), m_objectStreamFactory);
			final ClientLoginValidator clientLoginValidator = new ClientLoginValidator(m_serverMessenger);
			clientLoginValidator.setGamePassword(props.getPassword());
			m_serverMessenger.setLoginValidator(clientLoginValidator);
			m_serverMessenger.addErrorListener(this);
			m_serverMessenger.addConnectionChangeListener(this);
			final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(m_serverMessenger);
			m_remoteMessenger = new RemoteMessenger(unifiedMessenger);
			m_remoteMessenger.registerRemote(m_serverStartupRemote, SERVER_REMOTE_NAME);
			m_channelMessenger = new ChannelMessenger(unifiedMessenger);
			final NullModeratorController moderatorController = new NullModeratorController(m_serverMessenger);
			moderatorController.register(m_remoteMessenger);
			m_chatController = new ChatController(CHAT_NAME, m_serverMessenger, m_remoteMessenger, m_channelMessenger, moderatorController);
			if (ui == null && m_headless)
				m_chatPanel = new HeadlessChat(m_serverMessenger, m_channelMessenger, m_remoteMessenger, CHAT_NAME, Chat.CHAT_SOUND_PROFILE.GAME_CHATROOM);
			else
				m_chatPanel = new ChatPanel(m_serverMessenger, m_channelMessenger, m_remoteMessenger, CHAT_NAME, Chat.CHAT_SOUND_PROFILE.GAME_CHATROOM);
			m_serverMessenger.setAcceptNewConnections(true);
			gameDataChanged();
			return true;
		} catch (final IOException ioe)
		{
			ioe.printStackTrace(System.out);
			if (m_headless)
				System.out.println("Unable to create server socket:" + ioe.getMessage());
			else
				JOptionPane.showMessageDialog(ui, "Unable to create server socket:" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}
	
	private final IServerStartupRemote m_serverStartupRemote = new IServerStartupRemote()
	{
		public PlayerListing getPlayerListing()
		{
			return getPlayerListingInternal();
		}
		
		public void takePlayer(final INode who, final String playerName)
		{
			takePlayerInternal(who, true, playerName);
		}
		
		public void releasePlayer(final INode who, final String playerName)
		{
			takePlayerInternal(who, false, playerName);
		}
		
		public boolean isGameStarted(final INode newNode)
		{
			if (m_serverLauncher != null)
			{
				final IObserverWaitingToJoin observerWaitingToJoin = (IObserverWaitingToJoin) m_remoteMessenger.getRemote(getObserverWaitingToStartName(newNode));
				m_serverLauncher.addObserver(observerWaitingToJoin, newNode);
				return true;
			}
			else
			{
				return false;
			}
		}
		
		public boolean getIsServerHeadless()
		{
			return HeadlessGameServer.headless();
		}
		
		/**
		 * This should not be called from within game, only from the game setup screen, while everyone is waiting for game to start
		 */
		public byte[] getSaveGame()
		{
			System.out.println("Sending save game");
			final ByteArrayOutputStream sink = new ByteArrayOutputStream(5000);
			byte[] bytes = null;
			try
			{
				new GameDataManager().saveGame(sink, m_data);
				bytes = sink.toByteArray();
			} catch (final IOException e)
			{
				e.printStackTrace();
				throw new IllegalStateException(e);
			} finally
			{
				try
				{
					sink.close();
				} catch (final IOException e)
				{
				}
			}
			return bytes;
		}
		
		public Set<String> getAvailableGames()
		{
			final HeadlessGameServer headless = HeadlessGameServer.getInstance();
			if (headless == null)
				return null;
			return headless.getAvailableGames();
		}
		
		public void changeServerGameTo(final String gameName)
		{
			final HeadlessGameServer headless = HeadlessGameServer.getInstance();
			if (headless == null)
				return;
			System.out.println("Changing to game map: " + gameName);
			headless.setGameMapTo(gameName);
		}
		
		public void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave)
		{
			final HeadlessGameServer headless = HeadlessGameServer.getInstance();
			if (headless == null)
				return;
			final File save;
			if (SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE.equals(typeOfAutosave))
				save = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveFileName());
			else if (SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_ODD.equals(typeOfAutosave))
				save = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveOddFileName());
			else if (SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_EVEN.equals(typeOfAutosave))
				save = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveEvenFileName());
			else
				return;
			if (save == null || !save.exists())
			{
				return;
			}
			System.out.println("Changing to autosave of type: " + typeOfAutosave.toString());
			headless.loadGameSave(save);
		}
		
		public void changeToGameSave(final byte[] bytes, final String fileName)
		{
			// TODO: change to a string message return, so we can tell the user/requestor if it was successful or not, and why if not.
			final HeadlessGameServer headless = HeadlessGameServer.getInstance();
			if (headless == null || bytes == null)
				return;
			System.out.println("Changing to user savegame: " + fileName);
			ByteArrayInputStream input = null;
			InputStream oinput = null;
			try
			{
				input = new ByteArrayInputStream(bytes);
				oinput = new BufferedInputStream(input);
				headless.loadGameSave(oinput, fileName);
			} catch (final Exception e)
			{
				e.printStackTrace();
			} finally
			{
				if (input != null)
				{
					try
					{
						input.close();
					} catch (final IOException e)
					{
						e.printStackTrace();
					}
				}
				if (oinput != null)
				{
					try
					{
						oinput.close();
					} catch (final IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	};
	
	private PlayerListing getPlayerListingInternal()
	{
		synchronized (this)
		{
			if (m_data == null)
				return new PlayerListing(new HashMap<String, String>(), new Version(0, 0), m_gameSelectorModel.getGameName(), m_gameSelectorModel.getGameRound(),
							new LinkedHashMap<String, Collection<String>>());
			else
				return new PlayerListing(new HashMap<String, String>(m_players), m_data.getGameVersion(), m_data.getGameName(), m_data.getSequence().getRound() + "",
							m_playerNamesAndAlliancesInTurnOrder);
		}
	}
	
	private void takePlayerInternal(final INode from, final boolean take, final String playerName)
	{
		// synchronize to make sure two adds arent executed at once
		synchronized (this)
		{
			if (!m_players.containsKey(playerName))
				return;
			if (take)
				m_players.put(playerName, from.getName());
			else
				m_players.put(playerName, null);
		}
		notifyChanellPlayersChanged();
		m_listener.playersTakenChanged();
	}
	
	private void notifyChanellPlayersChanged()
	{
		final IClientChannel channel = (IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
		channel.playerListingChanged(getPlayerListingInternal());
	}
	
	public void takePlayer(final String playerName)
	{
		takePlayerInternal(m_serverMessenger.getLocalNode(), true, playerName);
	}
	
	public void releasePlayer(final String playerName)
	{
		takePlayerInternal(m_serverMessenger.getLocalNode(), false, playerName);
	}
	
	public IServerMessenger getMessenger()
	{
		return m_serverMessenger;
	}
	
	public Map<String, String> getPlayers()
	{
		synchronized (this)
		{
			return new HashMap<String, String>(m_players);
		}
	}
	
	public Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap()
	{
		synchronized (this)
		{
			return new LinkedHashMap<String, Collection<String>>(m_playerNamesAndAlliancesInTurnOrder);
		}
	}
	
	public void messengerInvalid(final IMessenger messenger, final Exception reason)
	{
		if (m_headless)
		{
			System.out.println("Connection Lost");
			if (m_typePanelModel != null)
				m_typePanelModel.showSelectType();
		}
		else
		{
			JOptionPane.showMessageDialog(m_ui, "Connection lost", "Error", JOptionPane.ERROR_MESSAGE);
			m_typePanelModel.showSelectType();
		}
	}
	
	public void connectionAdded(final INode to)
	{
	}
	
	public void connectionRemoved(final INode node)
	{
		// will be handled elsewhere
		if (m_serverLauncher != null)
		{
			m_serverLauncher.connectionLost(node);
			return;
		}
		// we lost a node. Remove the players he plays.
		final List<String> free = new ArrayList<String>();
		synchronized (this)
		{
			for (final String player : m_players.keySet())
			{
				final String playedBy = m_players.get(player);
				if (playedBy != null && playedBy.equals(node.getName()))
				{
					free.add(player);
				}
			}
		}
		for (final String player : free)
		{
			takePlayerInternal(node, false, player);
		}
	}
	
	public IChatPanel getChatPanel()
	{
		return m_chatPanel;
	}
	
	public ILauncher getLauncher()
	{
		synchronized (this)
		{
			// -1 since we dont count outselves
			final int clientCount = m_serverMessenger.getNodes().size() - 1;
			final Map<String, String> localPlayerMappings = new HashMap<String, String>();
			final Map<String, INode> remotePlayers = new HashMap<String, INode>();
			final String defaultLocalType = m_data.getGameLoader().getServerPlayerTypes()[0];
			for (final String player : m_players.keySet())
			{
				final String playedBy = m_players.get(player);
				if (playedBy.equals(m_serverMessenger.getLocalNode().getName()))
				{
					String type = defaultLocalType;
					if (m_localPlayerTypes.containsKey(player))
						type = m_localPlayerTypes.get(player);
					localPlayerMappings.put(player, type);
				}
				else
				{
					final Set<INode> nodes = m_serverMessenger.getNodes();
					for (final INode node : nodes)
					{
						if (node.getName().equals(playedBy))
						{
							remotePlayers.put(player, node);
							break;
						}
					}
				}
			}
			final ServerLauncher launcher = new ServerLauncher(clientCount, m_remoteMessenger, m_channelMessenger, m_serverMessenger, m_gameSelectorModel, localPlayerMappings, remotePlayers, this,
						m_headless);
			return launcher;
		}
	}
	
	public void newGame()
	{
		m_serverMessenger.setAcceptNewConnections(true);
		final IClientChannel channel = (IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME);
		notifyChanellPlayersChanged();
		channel.gameReset();
	}
	
	public void setServerLauncher(final ServerLauncher launcher)
	{
		m_serverLauncher = launcher;
	}
}


class ServerProps
{
	private String name;
	private int port;
	private String password;
	
	public String getPassword()
	{
		return password;
	}
	
	public void setPassword(final String password)
	{
		this.password = password;
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
