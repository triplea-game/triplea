package games.strategy.engine.framework.headlessGameServer;

import games.strategy.debug.DebugUtils;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.triplea.util.LoggingPrintStream;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

import java.awt.Dimension;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * A way of hosting a game, but headless.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
public class HeadlessGameServer
{
	/*{
		// we are not using this, in case the user wants to have a host bot with the host bot ui. so instead let them set it with: java -Djava.awt.headless=true
		// must be static, must be very first thing in the class:
		System.setProperty("java.awt.headless", "true");
		// System.out.println("Headless AWT Test: " + java.awt.GraphicsEnvironment.isHeadless());
	}*/
	public static final String TRIPLEA_GAME_HOST_UI_PROPERTY = "triplea.game.host.ui";
	public static final String TRIPLEA_HEADLESS = "triplea.headless";
	public static final String TRIPLEA_GAME_HOST_CONSOLE_PROPERTY = "triplea.game.host.console";
	final static Logger s_logger = Logger.getLogger(HeadlessGameServer.class.getName());
	static HeadlessGameServerConsole s_console = null;
	private static HeadlessGameServer s_instance = null;
	private final AvailableGames m_availableGames;
	private final GameSelectorModel m_gameSelectorModel;
	private SetupPanelModel m_setupPanelModel = null;
	private HeadlessServerMainPanel m_mainPanel = null;
	private final boolean m_useUI;
	private final ScheduledExecutorService m_lobbyWatcherResetupThread = Executors.newScheduledThreadPool(1);
	private ServerGame m_iGame = null;
	private boolean m_shutDown = false;
	@SuppressWarnings("deprecation")
	private final String m_startDate = new Date().toGMTString();
	private static final int LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM = 21600;
	private static final int LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT = 2 * LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM;
	private static final String NO_REMOTE_REQUESTS_ALLOWED = "noRemoteRequestsAllowed";
	
	public static String[] getProperties()
	{
		return new String[] { GameRunner2.TRIPLEA_GAME_PROPERTY, TRIPLEA_GAME_HOST_CONSOLE_PROPERTY, TRIPLEA_GAME_HOST_UI_PROPERTY, GameRunner2.TRIPLEA_SERVER_PROPERTY,
					GameRunner2.TRIPLEA_PORT_PROPERTY, GameRunner2.TRIPLEA_NAME_PROPERTY, GameRunner2.LOBBY_HOST, GameRunner2.LOBBY_PORT, GameRunner2.LOBBY_GAME_COMMENTS,
					GameRunner2.LOBBY_GAME_HOSTED_BY, GameRunner2.LOBBY_GAME_SUPPORT_EMAIL, GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, GameRunner2.LOBBY_GAME_RECONNECTION,
					GameRunner2.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME, GameRunner2.TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME };
	}
	
	private static void usage()
	{
		System.out.println("\nUsage and Valid Arguments:\n"
					+ "   " + GameRunner2.TRIPLEA_GAME_PROPERTY + "=<FILE_NAME>\n"
					+ "   " + TRIPLEA_GAME_HOST_CONSOLE_PROPERTY + "=<true/false>\n"
					+ "   " + TRIPLEA_GAME_HOST_UI_PROPERTY + "=<true/false>\n"
					+ "   " + GameRunner2.TRIPLEA_SERVER_PROPERTY + "=true\n"
					+ "   " + GameRunner2.TRIPLEA_PORT_PROPERTY + "=<PORT>\n"
					+ "   " + GameRunner2.TRIPLEA_NAME_PROPERTY + "=<PLAYER_NAME>\n"
					+ "   " + GameRunner2.LOBBY_HOST + "=<LOBBY_HOST>\n"
					+ "   " + GameRunner2.LOBBY_PORT + "=<LOBBY_PORT>\n"
					+ "   " + GameRunner2.LOBBY_GAME_COMMENTS + "=<LOBBY_GAME_COMMENTS>\n"
					+ "   " + GameRunner2.LOBBY_GAME_HOSTED_BY + "=<LOBBY_GAME_HOSTED_BY>\n"
					+ "   " + GameRunner2.LOBBY_GAME_SUPPORT_EMAIL + "=<youremail@emailprovider.com>\n"
					+ "   " + GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD + "=<password for remote actions, such as remote stop game>\n"
					+ "   " + GameRunner2.LOBBY_GAME_RECONNECTION + "=<seconds between refreshing lobby connection [min " + LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM + "]>\n"
					+ "   " + GameRunner2.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME + "=<seconds to wait for all clients to start the game>\n"
					+ "   " + GameRunner2.TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME + "=<seconds to wait for an observer joining the game>\n"
					+ "\n"
					+ "   You must start the Name and HostedBy with \"Bot\".\n"
					+ "   Game Comments must have this string in it: \"automated_host\".\n"
					+ "   You must include a support email for your host, so that you can be alerted by lobby admins when your host has an error."
					+ " (For example they may email you when your host is down and needs to be restarted.)\n"
					+ "   Support password is a remote access password that will allow lobby admins to remotely take the following actions: ban player, stop game, shutdown server."
					+ " (Please email this password to one of the lobby moderators, or private message an admin on the TripleaWarClub.org website forum.)\n");
	}
	
	public static synchronized HeadlessGameServer getInstance()
	{
		return s_instance;
	}
	
	public static synchronized boolean getUseGameServerUI()
	{
		return Boolean.parseBoolean(System.getProperty(TRIPLEA_GAME_HOST_UI_PROPERTY, "false"));
	}
	
	public static synchronized boolean headless()
	{
		if (getInstance() != null)
			return true;
		return Boolean.parseBoolean(System.getProperty(TRIPLEA_HEADLESS, "false"));
	}
	
	public Set<String> getAvailableGames()
	{
		return new HashSet<String>(m_availableGames.getGameNames());
	}
	
	public synchronized void setGameMapTo(final String gameName)
	{
		// don't change mid-game
		if (m_setupPanelModel.getPanel() != null && m_iGame == null)
		{
			if (!m_availableGames.getGameNames().contains(gameName))
				return;
			m_gameSelectorModel.load(m_availableGames.getGameData(gameName), m_availableGames.getGameFilePath(gameName));
			System.out.println("Changed to game map: " + gameName);
		}
	}
	
	public synchronized void loadGameSave(final File file)
	{
		// don't change mid-game
		if (m_setupPanelModel.getPanel() != null && m_iGame == null)
		{
			if (file == null || !file.exists())
				return;
			m_gameSelectorModel.load(file, null);
			System.out.println("Changed to save: " + file.getName());
		}
	}
	
	public synchronized void loadGameSave(final InputStream input, final String fileName)
	{
		// don't change mid-game
		if (m_setupPanelModel.getPanel() != null && m_iGame == null)
		{
			if (input == null || fileName == null)
				return;
			final GameData data = m_gameSelectorModel.getGameData(input, fileName);
			if (data == null)
			{
				System.out.println("Loading GameData failed for: " + fileName);
				return;
			}
			final String mapNameProperty = data.getProperties().get(Constants.MAP_NAME, "");
			if (!m_availableGames.getAvailableMapFolderOrZipNames().contains(mapNameProperty))
			{
				System.out.println("Game mapName not in available games listing: " + mapNameProperty);
				return;
			}
			m_gameSelectorModel.load(data, fileName);
			System.out.println("Changed to user savegame: " + fileName);
		}
	}
	
	public synchronized void loadGameSave(final ObjectInputStream input, final String fileName)
	{
		// don't change mid-game
		if (m_setupPanelModel.getPanel() != null && m_iGame == null)
		{
			if (input == null || fileName == null)
				return;
			final GameData data = m_gameSelectorModel.getGameData(input, fileName);
			if (data == null)
			{
				System.out.println("Loading GameData failed for: " + fileName);
				return;
			}
			final String mapNameProperty = data.getProperties().get(Constants.MAP_NAME, "");
			if (!m_availableGames.getAvailableMapFolderOrZipNames().contains(mapNameProperty))
			{
				System.out.println("Game mapName not in available games listing: " + mapNameProperty);
				return;
			}
			m_gameSelectorModel.load(data, fileName);
			System.out.println("Changed to user savegame: " + fileName);
		}
	}
	
	public synchronized void loadGameOptions(final byte[] bytes)
	{
		// don't change mid-game
		if (m_setupPanelModel.getPanel() != null && m_iGame == null)
		{
			if (bytes == null || bytes.length == 0)
				return;
			final GameData data = m_gameSelectorModel.getGameData();
			if (data == null)
				return;
			final GameProperties props = data.getProperties();
			if (props == null)
				return;
			GameProperties.applyByteMapToChangeProperties(bytes, props);
			System.out.println("Changed to user game options.");
		}
	}
	
	public static synchronized void setServerGame(final ServerGame serverGame)
	{
		final HeadlessGameServer instance = getInstance();
		if (instance != null)
		{
			instance.m_iGame = serverGame;
			if (serverGame != null)
			{
				System.out.println("Game starting up: " + instance.m_iGame.isGameSequenceRunning() + ", GameOver: " + instance.m_iGame.isGameOver() + ", Players: "
							+ instance.m_iGame.getPlayerManager().toString());
			}
		}
	}
	
	public static synchronized void log(final String stdout)
	{
		final HeadlessGameServer instance = getInstance();
		if (instance != null)
			System.out.println(stdout);
	}
	
	public static synchronized void sendChat(final String chatString)
	{
		final HeadlessGameServer instance = getInstance();
		if (instance != null)
		{
			final Chat chat = instance.getChat();
			if (chat != null)
			{
				try
				{
					chat.sendMessage(chatString, false);
				} catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public String getSalt()
	{
		final String encryptedPassword = MD5Crypt.crypt(System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, ""));
		final String salt = MD5Crypt.getSalt(MD5Crypt.MAGIC, encryptedPassword);
		return salt;
	}
	
	public String remoteShutdown(final String hashedPassword, final String salt)
	{
		final String password = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		if (password.equals(NO_REMOTE_REQUESTS_ALLOWED))
			return "Host not accepting remote requests!";
		final String localPassword = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
		if (encryptedPassword.equals(hashedPassword))
		{
			(new Thread(new Runnable()
			{
				public void run()
				{
					System.out.println("Remote Shutdown Initiated.");
					try
					{
						Thread.sleep(1000);
					} catch (final InterruptedException e)
					{
						e.printStackTrace();
					}
					System.exit(0);
				}
			})).start();
			return null;
		}
		System.out.println("Attempted remote shutdown with invalid password.");
		return "Invalid password!";
	}
	
	public String remoteStopGame(final String hashedPassword, final String salt)
	{
		final String password = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		if (password.equals(NO_REMOTE_REQUESTS_ALLOWED))
			return "Host not accepting remote requests!";
		final String localPassword = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
		if (encryptedPassword.equals(hashedPassword))
		{
			final ServerGame iGame = m_iGame;
			if (iGame != null)
			{
				(new Thread(new Runnable()
				{
					public void run()
					{
						System.out.println("Remote Stop Game Initiated.");
						SaveGameFileChooser.ensureDefaultDirExists();
						final File f1 = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSaveFileName());
						final File f2 = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, SaveGameFileChooser.getAutoSave2FileName());
						final File f;
						if (f1.lastModified() > f2.lastModified())
							f = f2;
						else
							f = f1;
						try
						{
							iGame.saveGame(f);
						} catch (final Exception e)
						{
							e.printStackTrace();
						}
						iGame.stopGame();
					}
				})).start();
			}
			return null;
		}
		System.out.println("Attempted remote stop game with invalid password.");
		return "Invalid password!";
	}
	
	public String remoteGetChatLog(final String hashedPassword, final String salt)
	{
		final String password = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		if (password.equals(NO_REMOTE_REQUESTS_ALLOWED))
			return "Host not accepting remote requests!";
		final String localPassword = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
		if (encryptedPassword.equals(hashedPassword))
		{
			final IChatPanel chat = getServerModel().getChatPanel();
			if (chat == null || chat.getAllText() == null)
				return "Empty or null chat";
			return chat.getAllText();
		}
		System.out.println("Attempted remote get chat log with invalid password.");
		return "Invalid password!";
	}
	
	public String remoteMutePlayer(final String playerName, final int minutes, final String hashedPassword, final String salt)
	{
		final String password = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		if (password.equals(NO_REMOTE_REQUESTS_ALLOWED))
			return "Host not accepting remote requests!";
		final String localPassword = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
		final long expire = System.currentTimeMillis() + (Math.max(0, Math.min(60 * 24 * 2, minutes)) * 1000 * 60); // milliseconds (48 hours max)
		if (encryptedPassword.equals(hashedPassword))
		{
			(new Thread(new Runnable()
			{
				public void run()
				{
					if (getServerModel() == null)
						return;
					final IServerMessenger messenger = getServerModel().getMessenger();
					if (messenger == null)
						return;
					final Set<INode> nodes = messenger.getNodes();
					if (nodes == null)
						return;
					try
					{
						for (final INode node : nodes)
						{
							final String realName = node.getName().split(" ")[0];
							final String ip = node.getAddress().getHostAddress();
							final String mac = messenger.GetPlayerMac(node.getName());
							if (realName.equals(playerName))
							{
								System.out.println("Remote Mute of Player: " + playerName);
								messenger.NotifyUsernameMutingOfPlayer(realName, new Date(expire));
								messenger.NotifyIPMutingOfPlayer(ip, new Date(expire));
								messenger.NotifyMacMutingOfPlayer(mac, new Date(expire));
								return;
							}
						}
					} catch (final Exception e)
					{
						e.printStackTrace();
					}
				}
			})).start();
			return null;
		}
		System.out.println("Attempted remote mute player with invalid password.");
		return "Invalid password!";
	}
	
	public String remoteBootPlayer(final String playerName, final String hashedPassword, final String salt)
	{
		final String password = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		if (password.equals(NO_REMOTE_REQUESTS_ALLOWED))
			return "Host not accepting remote requests!";
		final String localPassword = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
		if (encryptedPassword.equals(hashedPassword))
		{
			(new Thread(new Runnable()
			{
				public void run()
				{
					if (getServerModel() == null)
						return;
					final IServerMessenger messenger = getServerModel().getMessenger();
					if (messenger == null)
						return;
					final Set<INode> nodes = messenger.getNodes();
					if (nodes == null)
						return;
					try
					{
						for (final INode node : nodes)
						{
							final String realName = node.getName().split(" ")[0];
							if (realName.equals(playerName))
							{
								System.out.println("Remote Boot of Player: " + playerName);
								messenger.removeConnection(node);
							}
						}
					} catch (final Exception e)
					{
						e.printStackTrace();
					}
				}
			})).start();
			return null;
		}
		System.out.println("Attempted remote boot player with invalid password.");
		return "Invalid password!";
	}
	
	public String remoteBanPlayer(final String playerName, final int hours, final String hashedPassword, final String salt)
	{
		final String password = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		if (password.equals(NO_REMOTE_REQUESTS_ALLOWED))
			return "Host not accepting remote requests!";
		final String localPassword = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, "");
		final String encryptedPassword = MD5Crypt.crypt(localPassword, salt);
		final long expire = System.currentTimeMillis() + (Math.max(0, Math.min(24 * 30, hours)) * 1000 * 60 * 60); // milliseconds (30 days max)
		if (encryptedPassword.equals(hashedPassword))
		{
			(new Thread(new Runnable()
			{
				public void run()
				{
					if (getServerModel() == null)
						return;
					final IServerMessenger messenger = getServerModel().getMessenger();
					if (messenger == null)
						return;
					final Set<INode> nodes = messenger.getNodes();
					if (nodes == null)
						return;
					try
					{
						for (final INode node : nodes)
						{
							final String realName = node.getName().split(" ")[0];
							final String ip = node.getAddress().getHostAddress();
							final String mac = messenger.GetPlayerMac(node.getName());
							if (realName.equals(playerName))
							{
								System.out.println("Remote Ban of Player: " + playerName);
								try
								{
									messenger.NotifyUsernameMiniBanningOfPlayer(realName, new Date(expire));
								} catch (final Exception e)
								{
									e.printStackTrace();
								}
								try
								{
									messenger.NotifyIPMiniBanningOfPlayer(ip, new Date(expire));
								} catch (final Exception e)
								{
									e.printStackTrace();
								}
								try
								{
									messenger.NotifyMacMiniBanningOfPlayer(mac, new Date(expire));
								} catch (final Exception e)
								{
									e.printStackTrace();
								}
								messenger.removeConnection(node);
							}
						}
					} catch (final Exception e)
					{
						e.printStackTrace();
					}
				}
			})).start();
			return null;
		}
		System.out.println("Attempted remote ban player with invalid password.");
		return "Invalid password!";
	}
	
	ServerGame getIGame()
	{
		return m_iGame;
	}
	
	public boolean isShutDown()
	{
		return m_shutDown;
	}
	
	public HeadlessGameServer(final boolean useUI)
	{
		super();
		if (s_instance != null)
			throw new IllegalStateException("Instance already exists");
		s_instance = this;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{
			public void run()
			{
				System.out.println("Running ShutdownHook.");
				shutdown();
			}
		}));
		m_useUI = useUI;
		m_availableGames = new AvailableGames();
		m_gameSelectorModel = new GameSelectorModel();
		final String fileName = System.getProperty(GameRunner2.TRIPLEA_GAME_PROPERTY, "");
		if (fileName.length() > 0)
		{
			try
			{
				final File file = new File(fileName);
				m_gameSelectorModel.load(file, null);
			} catch (final Exception e)
			{
				m_gameSelectorModel.resetGameDataToNull();
			}
		}
		if (m_useUI)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					System.out.println("Starting UI");
					final JFrame frame = new JFrame("TripleA Headless Game Server UI Main Frame");
					frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					frame.setPreferredSize(new Dimension(700, 630));
					frame.setSize(new Dimension(700, 630));
					frame.setLocationRelativeTo(null);
					m_setupPanelModel = new HeadlessServerSetupPanelModel(m_gameSelectorModel, frame);
					m_setupPanelModel.showSelectType();
					m_mainPanel = new HeadlessServerMainPanel(m_setupPanelModel, m_availableGames);
					frame.getContentPane().add(m_mainPanel);
					frame.pack();
					frame.setVisible(true);
					frame.toFront();
					System.out.println("Waiting for users to connect.");
				}
			});
		}
		else
		{
			final Runnable r = new Runnable()
			{
				public void run()
				{
					System.out.println("Headless Start");
					m_setupPanelModel = new HeadlessServerSetupPanelModel(m_gameSelectorModel, null);
					m_setupPanelModel.showSelectType();
					System.out.println("Waiting for users to connect.");
					waitForUsersHeadless();
				}
			};
			final Thread t = new Thread(r, "Initialize Headless Server Setup Model");
			t.start();
		}
		int reconnect;
		try
		{
			final String reconnectionSeconds = System.getProperty(GameRunner2.LOBBY_GAME_RECONNECTION, "" + LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT);
			reconnect = Math.max(Integer.parseInt(reconnectionSeconds), LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM);
		} catch (final NumberFormatException e)
		{
			reconnect = LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT;
		}
		m_lobbyWatcherResetupThread.scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				try
				{
					restartLobbyWatcher(m_setupPanelModel, m_iGame);
				} catch (final Exception e)
				{
					try
					{
						Thread.sleep(10 * 60 * 1000);
					} catch (final InterruptedException e1)
					{
					}
					restartLobbyWatcher(m_setupPanelModel, m_iGame); // try again, but don't catch it this time
				}
			}
		}, reconnect, reconnect, TimeUnit.SECONDS);
		s_logger.info("Game Server initialized");
	}
	
	private static synchronized void restartLobbyWatcher(final SetupPanelModel setupPanelModel, final ServerGame iGame)
	{
		try
		{
			final ISetupPanel setup = setupPanelModel.getPanel();
			if (setup == null)
				return;
			if (iGame != null)
				return;
			if (setup.canGameStart())
				return;
			if (setup instanceof ServerSetupPanel)
			{
				((ServerSetupPanel) setup).repostLobbyWatcher(iGame);
			}
			else if (setup instanceof HeadlessServerSetup)
			{
				((HeadlessServerSetup) setup).repostLobbyWatcher(iGame);
			}
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void resetLobbyHostOldExtensionProperties()
	{
		for (final String property : getProperties())
		{
			if (GameRunner2.LOBBY_HOST.equals(property) || GameRunner2.LOBBY_PORT.equals(property) || GameRunner2.LOBBY_GAME_HOSTED_BY.equals(property))
			{
				// for these 3 properties, we clear them after hosting, but back them up.
				final String oldValue = System.getProperty(property + GameRunner2.OLD_EXTENSION);
				if (oldValue != null)
				{
					System.setProperty(property, oldValue);
				}
			}
		}
	}
	
	public String getStatus()
	{
		String message = "Server Start Date: " + m_startDate;
		final ServerGame game = getIGame();
		if (game != null)
		{
			message += "\nIs currently running: " + game.isGameSequenceRunning() + "\nIs GameOver: " + game.isGameOver()
						+ "\nGame: " + game.getData().getGameName() + "\nRound: " + game.getData().getSequence().getRound()
						+ "\nPlayers: " + game.getPlayerManager().toString();
		}
		else
		{
			message += "\nCurrently Waiting To Start A Game";
		}
		return message;
	}
	
	public void printThreadDumpsAndStatus()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("Dump to Log:");
		sb.append("\n\nStatus:\n");
		sb.append(getStatus());
		sb.append("\n\nServer:\n");
		sb.append(getServerModel());
		sb.append("\n\n");
		sb.append(DebugUtils.getThreadDumps());
		sb.append("\n\n");
		sb.append(DebugUtils.getMemory());
		sb.append("\n\nDump finished.\n");
		System.out.println(sb.toString());
	}
	
	public synchronized void shutdown()
	{
		m_shutDown = true;
		printThreadDumpsAndStatus();
		try
		{
			if (m_lobbyWatcherResetupThread != null)
			{
				m_lobbyWatcherResetupThread.shutdown();
			}
		} catch (final Exception e)
		{
		}
		try
		{
			if (m_iGame != null)
			{
				m_iGame.stopGame();
			}
		} catch (final Exception e)
		{
		}
		try
		{
			if (m_setupPanelModel != null)
			{
				final ISetupPanel setup = m_setupPanelModel.getPanel();
				if (setup != null && setup instanceof ServerSetupPanel)
				{
					// ((ServerSetupPanel) setup).shutDown();// this is causing a deadlock when in a shutdown hook, due to swing/awt
				}
				else if (setup != null && setup instanceof HeadlessServerSetup)
				{
					((HeadlessServerSetup) setup).shutDown();
				}
			}
		} catch (final Exception e)
		{
		}
		try
		{
			if (m_gameSelectorModel != null && m_gameSelectorModel.getGameData() != null)
			{
				m_gameSelectorModel.getGameData().clearAllListeners();
			}
		} catch (final Exception e)
		{
		}
		/*// this is causing a deadlock when in a shutdown hook, due to swing/awt
		try
		{
			if (m_mainPanel != null)
			{
				m_mainPanel.setVisible(false);
				final Frame frame = JOptionPane.getFrameForComponent(m_mainPanel);
				m_mainPanel.removeAll();
				frame.setVisible(false);
				frame.removeAll();
				frame.dispose();
				m_mainPanel = null;
			}
		} catch (final Exception e)
		{
		}*/
		s_instance = null;
		m_setupPanelModel = null;
		m_mainPanel = null;
		m_iGame = null;
		System.out.println("Shutdown Script Finished.");
	}
	
	public void waitForUsersHeadless()
	{
		setServerGame(null);
		if (m_useUI)
			return;
		final Runnable r = new Runnable()
		{
			public void run()
			{
				while (!m_shutDown)
				{
					try
					{
						Thread.sleep(8000);
					} catch (final InterruptedException e)
					{
					}
					if (m_setupPanelModel != null && m_setupPanelModel.getPanel() != null && m_setupPanelModel.getPanel().canGameStart())
					{
						final boolean started = startHeadlessGame(m_setupPanelModel);
						if (!started)
							System.out.println("Error in launcher, going back to waiting.");
						else
							break; // TODO: need a latch instead?
					}
				}
			}
		};
		final Thread t = new Thread(r, "Headless Server Waiting For Users To Connect And Start");
		t.start();
	}
	
	private synchronized static boolean startHeadlessGame(final SetupPanelModel setupPanelModel)
	{
		try
		{
			if (setupPanelModel != null && setupPanelModel.getPanel() != null && setupPanelModel.getPanel().canGameStart())
			{
				ErrorHandler.setGameOver(false);
				System.out.println("Starting Game: " + setupPanelModel.getGameSelectorModel().getGameData().getGameName() + ", Round: "
							+ setupPanelModel.getGameSelectorModel().getGameData().getSequence().getRound());
				setupPanelModel.getPanel().preStartGame();
				final ILauncher launcher = setupPanelModel.getPanel().getLauncher();
				if (launcher != null)
					launcher.launch(null);
				setupPanelModel.getPanel().postStartGame();
				return launcher != null;
			}
		} catch (final Exception e)
		{
			e.printStackTrace();
			final ServerModel model = getServerModel(setupPanelModel);
			if (model != null)
				model.setAllPlayersToNullNodes();// if we do not do this, we can get into an infinite loop of launching a game, then crashing out, then launching, etc.
		}
		return false;
	}
	
	public static void waitForUsersHeadlessInstance()
	{
		final HeadlessGameServer server = getInstance();
		if (server == null)
		{
			System.err.println("Couldn't find instance.");
			System.exit(-1);
		}
		else
		{
			System.out.println("Waiting for users to connect.");
			server.waitForUsersHeadless();
		}
	}
	
	SetupPanelModel getSetupPanelModel()
	{
		return m_setupPanelModel;
	}
	
	ServerModel getServerModel()
	{
		return getServerModel(m_setupPanelModel);
	}
	
	static ServerModel getServerModel(final SetupPanelModel setupPanelModel)
	{
		if (setupPanelModel == null)
			return null;
		final ISetupPanel setup = setupPanelModel.getPanel();
		if (setup == null)
			return null;
		if (setup instanceof ServerSetupPanel)
		{
			return ((ServerSetupPanel) setup).getModel();
		}
		else if (setup instanceof HeadlessServerSetup)
		{
			return ((HeadlessServerSetup) setup).getModel();
		}
		return null;
	}
	
	/**
	 * todo, replace with something better
	 * 
	 * Get the chat for the game, or null if there is no chat
	 */
	public Chat getChat()
	{
		final ISetupPanel model = m_setupPanelModel.getPanel();
		if (model instanceof ServerSetupPanel)
		{
			return model.getChatPanel().getChat();
		}
		else if (model instanceof ClientSetupPanel)
		{
			return model.getChatPanel().getChat();
		}
		else if (model instanceof HeadlessServerSetup)
		{
			return model.getChatPanel().getChat();
		}
		else
		{
			return null;
		}
	}
	
	public static void main(final String[] args)
	{
		System.out.println("Headless AWT Test: " + java.awt.GraphicsEnvironment.isHeadless());
		handleCommandLineArgs(args);
		// grab these before we override them with the loggers
		final InputStream in = System.in;
		final PrintStream out = System.out;
		setupLogging();// after handling the command lines, because we use the triplea.game.name= property in our log file name
		final boolean startUI = getUseGameServerUI();
		if (!startUI)
		{
			ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
		}
		HeadlessGameServer server = null;
		try
		{
			server = new HeadlessGameServer(startUI);
		} catch (final Exception e)
		{
			e.printStackTrace();
			// main(new String[] {});
		}
		if (Boolean.parseBoolean(System.getProperty(TRIPLEA_GAME_HOST_CONSOLE_PROPERTY, "false")))
		{
			startConsole(server, in, out);
		}
	}
	
	private static void startConsole(final HeadlessGameServer server, final InputStream in, final PrintStream out)
	{
		System.out.println("Starting console.");
		s_console = new HeadlessGameServerConsole(server, in, out);
		s_console.start();
	}
	
	public static void setupLogging()
	{
		// setup logging to read our logging.properties
		try
		{
			LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("headless-game-server-logging.properties"));
			Logger.getAnonymousLogger().info("Redirecting std out");
			System.setErr(new LoggingPrintStream("ERROR", Level.SEVERE));
			System.setOut(new LoggingPrintStream("OUT", Level.INFO));
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Move command line arguments to System.properties
	 */
	private static void handleCommandLineArgs(final String[] args)
	{
		System.getProperties().setProperty(TRIPLEA_HEADLESS, "true");
		final String[] properties = getProperties();
		// if only 1 arg, it might be the game path, find it (like if we are double clicking a savegame)
		// optionally, it may not start with the property name
		if (args.length == 1)
		{
			boolean startsWithPropertyKey = false;
			for (final String prop : properties)
			{
				if (args[0].startsWith(prop))
				{
					startsWithPropertyKey = true;
					break;
				}
			}
			if (!startsWithPropertyKey)
			{
				// change it to start with the key
				args[0] = GameRunner2.TRIPLEA_GAME_PROPERTY + "=" + args[0];
			}
		}
		
		boolean printUsage = false;
		for (int argIndex = 0; argIndex < args.length; argIndex++)
		{
			boolean found = false;
			String arg = args[argIndex];
			final int indexOf = arg.indexOf('=');
			if (indexOf > 0)
			{
				arg = arg.substring(0, indexOf);
				for (int propIndex = 0; propIndex < properties.length; propIndex++)
				{
					if (arg.equals(properties[propIndex]))
					{
						final String value = getValue(args[argIndex]);
						System.getProperties().setProperty(properties[propIndex], value);
						System.out.println(properties[propIndex] + ":" + value);
						found = true;
						break;
					}
				}
			}
			if (!found)
			{
				System.out.println("Unrecogized argument: " + args[argIndex]);
				printUsage = true;
			}
		}
		
		{ // now check for required fields
			final String playerName = System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY, "");
			final String hostName = System.getProperty(GameRunner2.LOBBY_GAME_HOSTED_BY, "");
			final String comments = System.getProperty(GameRunner2.LOBBY_GAME_COMMENTS, "");
			final String email = System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_EMAIL, "");
			final String reconnection = System.getProperty(GameRunner2.LOBBY_GAME_RECONNECTION, "" + LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT);
			if (playerName.length() < 7 || hostName.length() < 7 || !hostName.equals(playerName) || !playerName.startsWith("Bot") || !hostName.startsWith("Bot"))
			{
				System.out.println("Invalid argument: " + GameRunner2.TRIPLEA_NAME_PROPERTY + " and " + GameRunner2.LOBBY_GAME_HOSTED_BY
							+ " must start with \"Bot\" and be at least 7 characters long and be the same.");
				printUsage = true;
			}
			if (comments.indexOf("automated_host") == -1)
			{
				System.out.println("Invalid argument: " + GameRunner2.LOBBY_GAME_COMMENTS + " must contain the string \"automated_host\".");
				printUsage = true;
			}
			if (email.length() < 3 || !Util.isMailValid(email))
			{
				System.out.println("Invalid argument: " + GameRunner2.LOBBY_GAME_SUPPORT_EMAIL + " must contain a valid email address.");
				printUsage = true;
			}
			try
			{
				final int reconnect = Integer.parseInt(reconnection);
				if (reconnect < LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM)
				{
					System.out.println("Invalid argument: " + GameRunner2.LOBBY_GAME_RECONNECTION + " must be an integer equal to or greater than " + LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM
								+ " seconds, and should normally be either " + LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT + " or " + (2 * LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT) + " seconds.");
					printUsage = true;
				}
			} catch (final NumberFormatException e)
			{
				System.out.println("Invalid argument: " + GameRunner2.LOBBY_GAME_RECONNECTION + " must be an integer equal to or greater than " + LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM
							+ " seconds, and should normally be either " + LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT + " or " + (2 * LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT) + " seconds.");
				printUsage = true;
			}
			// no passwords allowed for bots
		}
		{// take any actions or commit to preferences
			final String clientWait = System.getProperty(GameRunner2.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME, "");
			final String observerWait = System.getProperty(GameRunner2.TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME, "");
			if (clientWait.length() > 0)
			{
				try
				{
					final int wait = Integer.parseInt(clientWait);
					GameRunner2.setServerStartGameSyncWaitTime(wait);
				} catch (final NumberFormatException e)
				{
					System.out.println("Invalid argument: " + GameRunner2.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME + " must be an integer.");
					printUsage = true;
				}
			}
			if (observerWait.length() > 0)
			{
				try
				{
					final int wait = Integer.parseInt(observerWait);
					GameRunner2.setServerObserverJoinWaitTime(wait);
				} catch (final NumberFormatException e)
				{
					System.out.println("Invalid argument: " + GameRunner2.TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME + " must be an integer.");
					printUsage = true;
				}
			}
		}
		if (printUsage)
		{
			usage();
			System.exit(-1);
		}
	}
	
	private static String getValue(final String arg)
	{
		final int index = arg.indexOf('=');
		if (index == -1)
			return "";
		return arg.substring(index + 1);
	}
}
