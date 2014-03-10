package games.strategy.engine.framework;

import games.strategy.common.ui.InGameLobbyWatcherWrapper;
import games.strategy.debug.DebugUtils;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.HeadlessChat;
import games.strategy.engine.chat.IChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.IRemoteModelListener;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.startup.ui.MetaSetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.startup.ui.SetupPanel;
import games.strategy.engine.framework.ui.NewGameChooserModel;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.triplea.util.LoggingPrintStream;
import games.strategy.util.ClassLoaderUtil;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

/**
 * Ideally a way of hosting a game, but headless.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
public class HeadlessGameServer
{
	/* 
	{
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
	private static final int LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT = 21600;
	private static final String NO_REMOTE_REQUESTS_ALLOWED = "noRemoteRequestsAllowed";
	
	public static String[] getProperties()
	{
		return new String[] { GameRunner2.TRIPLEA_GAME_PROPERTY, TRIPLEA_GAME_HOST_CONSOLE_PROPERTY, TRIPLEA_GAME_HOST_UI_PROPERTY, GameRunner2.TRIPLEA_SERVER_PROPERTY,
					GameRunner2.TRIPLEA_PORT_PROPERTY, GameRunner2.TRIPLEA_NAME_PROPERTY, GameRunner2.LOBBY_HOST, GameRunner2.LOBBY_PORT, GameRunner2.LOBBY_GAME_COMMENTS,
					GameRunner2.LOBBY_GAME_HOSTED_BY, GameRunner2.LOBBY_GAME_SUPPORT_EMAIL, GameRunner2.LOBBY_GAME_SUPPORT_PASSWORD, GameRunner2.LOBBY_GAME_RECONNECTION };
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
					+ "   " + GameRunner2.LOBBY_GAME_RECONNECTION + "=<seconds between refreshing lobby connection [min " + LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT + "]>\n"
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
						m_iGame.saveGame(f);
					} catch (final Exception e)
					{
						e.printStackTrace();
					}
					m_iGame.stopGame(false);
				}
			})).start();
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
					frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
			reconnect = Math.max(Integer.parseInt(reconnectionSeconds), LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT);
		} catch (final NumberFormatException e)
		{
			reconnect = LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT;
		}
		m_lobbyWatcherResetupThread.scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				restartLobbyWatcher(m_setupPanelModel, m_iGame);
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
	
	public synchronized void shutdown()
	{
		m_shutDown = true;
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
					((ServerSetupPanel) setup).shutDown();
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
		}
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
				if (reconnect < LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT)
				{
					System.out.println("Invalid argument: " + GameRunner2.LOBBY_GAME_RECONNECTION + " must be an integer equal to or greater than " + LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT
								+ " seconds.");
					printUsage = true;
				}
			} catch (final NumberFormatException e)
			{
				System.out.println("Invalid argument: " + GameRunner2.LOBBY_GAME_RECONNECTION + " must be an integer equal to or greater than " + LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT
							+ " seconds.");
				printUsage = true;
			}
			// no passwords allowed for bots
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


class HeadlessServerSetup implements IRemoteModelListener, ISetupPanel
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
			Thread.sleep(2000);
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


class HeadlessServerSetupPanelModel extends SetupPanelModel
{
	protected final Component m_ui;
	
	public HeadlessServerSetupPanelModel(final GameSelectorModel gameSelectorModel, final Component ui)
	{
		super(gameSelectorModel);
		m_ui = ui;
	}
	
	@Override
	public void showSelectType()
	{
		final ServerModel model = new ServerModel(m_gameSelectorModel, this, true);
		if (!model.createServerMessenger(m_ui))
		{
			model.cancel();
			return;
		}
		if (m_ui == null)
		{
			final HeadlessServerSetup serverSetup = new HeadlessServerSetup(model, m_gameSelectorModel);
			setGameTypePanel(serverSetup);
		}
		else
		{
			final ServerSetupPanel serverSetupPanel = new ServerSetupPanel(model, m_gameSelectorModel);
			setGameTypePanel(serverSetupPanel);
		}
	}
}


class HeadlessServerMainPanel extends JPanel implements Observer
{
	private static final long serialVersionUID = 1932202117432783020L;
	private JScrollPane m_gameSetupPanelScroll;
	private HeadlessGameSelectorPanel m_gameSelectorPanel;
	private JButton m_playButton;
	private JButton m_quitButton;
	// private JButton m_cancelButton;
	private final GameSelectorModel m_gameSelectorModel;
	private ISetupPanel m_gameSetupPanel;
	private JPanel m_gameSetupPanelHolder;
	private JPanel m_chatPanelHolder;
	private final SetupPanelModel m_gameTypePanelModel;
	private final JPanel m_mainPanel = new JPanel();
	private JSplitPane m_chatSplit;
	private static final Dimension m_initialSize = new Dimension(685, 620);
	private boolean m_isChatShowing;
	
	public HeadlessServerMainPanel(final SetupPanelModel typePanelModel, final AvailableGames availableGames)
	{
		m_gameTypePanelModel = typePanelModel;
		m_gameSelectorModel = typePanelModel.getGameSelectorModel();
		createComponents(availableGames);
		layoutComponents();
		setupListeners();
		setWidgetActivation();
		if (typePanelModel.getPanel() != null)
		{
			setGameSetupPanel(typePanelModel.getPanel());
		}
	}
	
	JButton getDefaultButton()
	{
		return m_playButton;
	}
	
	private void createComponents(final AvailableGames availableGames)
	{
		m_playButton = new JButton("Play");
		m_quitButton = new JButton("Quit");
		// m_cancelButton = new JButton("Cancel");
		m_gameSelectorPanel = new HeadlessGameSelectorPanel(m_gameSelectorModel, availableGames);
		m_gameSelectorPanel.setBorder(new EtchedBorder());
		m_gameSetupPanelHolder = new JPanel();
		m_gameSetupPanelHolder.setLayout(new BorderLayout());
		m_gameSetupPanelScroll = new JScrollPane(m_gameSetupPanelHolder);
		m_gameSetupPanelScroll.setBorder(BorderFactory.createEmptyBorder());
		m_chatPanelHolder = new JPanel();
		m_chatPanelHolder.setLayout(new BorderLayout());
		m_chatSplit = new JSplitPane();
		m_chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
		m_chatSplit.setResizeWeight(0.8);
		m_chatSplit.setOneTouchExpandable(false);
		m_chatSplit.setDividerSize(5);
	}
	
	private void layoutComponents()
	{
		final JPanel buttonsPanel = new JPanel();
		buttonsPanel.setBorder(new EtchedBorder());
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttonsPanel.add(m_playButton);
		buttonsPanel.add(m_quitButton);
		setLayout(new BorderLayout());
		m_mainPanel.setLayout(new GridBagLayout());
		m_mainPanel.setBorder(BorderFactory.createEmptyBorder());
		m_gameSetupPanelHolder.setLayout(new BorderLayout());
		m_mainPanel.add(m_gameSelectorPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(00, 0, 0, 0), 0, 0));
		m_mainPanel.add(m_gameSetupPanelScroll, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(00, 0, 0, 0), 0, 0));
		addChat();
		add(buttonsPanel, BorderLayout.SOUTH);
		setPreferredSize(m_initialSize);
	}
	
	private void addChat()
	{
		remove(m_mainPanel);
		remove(m_chatSplit);
		m_chatPanelHolder.removeAll();
		final ChatPanel chat;
		if (m_gameTypePanelModel != null && m_gameTypePanelModel.getPanel() != null && m_gameTypePanelModel.getPanel().getChatPanel() != null
					&& m_gameTypePanelModel.getPanel().getChatPanel() instanceof ChatPanel)
		{
			chat = (ChatPanel) m_gameTypePanelModel.getPanel().getChatPanel();
			m_chatPanelHolder = new JPanel();
			m_chatPanelHolder.setLayout(new BorderLayout());
			m_chatPanelHolder.add(chat, BorderLayout.CENTER);
			m_chatSplit.setTopComponent(m_mainPanel);
			m_chatSplit.setBottomComponent(m_chatPanelHolder);
			add(m_chatSplit, BorderLayout.CENTER);
			m_chatPanelHolder.setPreferredSize(new Dimension(m_chatPanelHolder.getPreferredSize().width, 62));
		}
		else
		{
			chat = null;
			add(m_mainPanel, BorderLayout.CENTER);
		}
		m_isChatShowing = chat != null;
	}
	
	public void setGameSetupPanel(final ISetupPanel panel)
	{
		SetupPanel setupPanel = null;
		if (SetupPanel.class.isAssignableFrom(panel.getClass()))
			setupPanel = (SetupPanel) panel;
		if (m_gameSetupPanel != null)
		{
			m_gameSetupPanel.removeObserver(this);
			if (setupPanel != null)
				m_gameSetupPanelHolder.remove(setupPanel);
		}
		m_gameSetupPanel = panel;
		m_gameSetupPanelHolder.removeAll();
		if (setupPanel != null)
			m_gameSetupPanelHolder.add(setupPanel, BorderLayout.CENTER);
		panel.addObserver(this);
		setWidgetActivation();
		// add the cancel button if we are not choosing the type.
		if (!(panel instanceof MetaSetupPanel))
		{
			final JPanel cancelPanel = new JPanel();
			cancelPanel.setBorder(new EmptyBorder(10, 0, 10, 10));
			cancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			createUserActionMenu(cancelPanel);
			// cancelPanel.add(m_cancelButton);
			m_gameSetupPanelHolder.add(cancelPanel, BorderLayout.SOUTH);
		}
		final boolean panelHasChat = (m_gameTypePanelModel.getPanel().getChatPanel() != null);
		if (panelHasChat != m_isChatShowing)
			addChat();
		invalidate();
		revalidate();
	}
	
	private void createUserActionMenu(final JPanel cancelPanel)
	{
		if (m_gameSetupPanel.getUserActions() == null)
			return;
		// if we need this for something other than network, add a way to set it
		final JButton button = new JButton("Network...");
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final JPopupMenu menu = new JPopupMenu();
				final List<Action> actions = m_gameSetupPanel.getUserActions();
				if (actions != null && !actions.isEmpty())
				{
					for (final Action a : actions)
					{
						menu.add(a);
					}
				}
				menu.show(button, 0, button.getHeight());
			}
		});
		cancelPanel.add(button);
	}
	
	private void setupListeners()
	{
		m_gameTypePanelModel.addObserver(new Observer()
		{
			public void update(final Observable o, final Object arg)
			{
				setGameSetupPanel(m_gameTypePanelModel.getPanel());
			}
		});
		m_playButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				play();
			}
		});
		m_quitButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				try
				{
					m_gameSetupPanel.shutDown();
				} finally
				{
					System.exit(0);
				}
			}
		});
		/*m_cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_gameTypePanelModel.showSelectType();
			}
		});*/
		m_gameSelectorModel.addObserver(this);
	}
	
	private void play()
	{
		ErrorHandler.setGameOver(false);
		System.out.println("Starting Game: " + m_gameSelectorModel.getGameData().getGameName() + ", Round: " + m_gameSelectorModel.getGameData().getSequence().getRound());
		m_gameSetupPanel.preStartGame();
		final ILauncher launcher = m_gameTypePanelModel.getPanel().getLauncher();
		if (launcher != null)
			launcher.launch(this);
		m_gameSetupPanel.postStartGame();
	}
	
	private void setWidgetActivation()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setWidgetActivation();
				}
			});
			return;
		}
		m_gameTypePanelModel.setWidgetActivation();
		if (m_gameSetupPanel != null)
		{
			m_playButton.setEnabled(m_gameSetupPanel.canGameStart());
		}
		else
		{
			m_playButton.setEnabled(false);
		}
	}
	
	public void update(final Observable o, final Object arg)
	{
		setWidgetActivation();
	}
}


class AvailableGames
{
	private static final boolean s_delayedParsing = false;
	private static final String ZIP_EXTENSION = ".zip";
	private final TreeMap<String, URI> m_availableGames = new TreeMap<String, URI>();
	private final Set<String> m_availableMapFolderOrZipNames = new HashSet<String>();
	
	public AvailableGames()
	{
		final Set<String> mapNamePropertyList = new HashSet<String>();
		populateAvailableGames(m_availableGames, m_availableMapFolderOrZipNames, mapNamePropertyList);
		// System.out.println(mapNamePropertyList);
		// System.out.println(m_availableMapFolderOrZipNames);
		m_availableMapFolderOrZipNames.retainAll(mapNamePropertyList);
		// System.out.println(m_availableMapFolderOrZipNames);
	}
	
	public List<String> getGameNames()
	{
		return new ArrayList<String>(m_availableGames.keySet());
	}
	
	public Set<String> getAvailableMapFolderOrZipNames()
	{
		return new HashSet<String>(m_availableMapFolderOrZipNames);
	}
	
	/**
	 * Can return null.
	 */
	public GameData getGameData(final String gameName)
	{
		return getGameDataFromXML(m_availableGames.get(gameName));
	}
	
	public URI getGameURI(final String gameName)
	{
		return m_availableGames.get(gameName);
	}
	
	public String getGameFilePath(final String gameName)
	{
		return getGameXMLLocation(m_availableGames.get(gameName));
	}
	
	private static void populateAvailableGames(final Map<String, URI> availableGames, final Set<String> availableMapFolderOrZipNames, final Set<String> mapNamePropertyList)
	{
		System.out.println("Parsing all available games (this could take a while). ");
		for (final File map : allMapFiles())
		{
			if (map.isDirectory())
			{
				populateFromDirectory(map, availableGames, availableMapFolderOrZipNames, mapNamePropertyList);
			}
			else if (map.isFile() && map.getName().toLowerCase().endsWith(ZIP_EXTENSION))
			{
				populateFromZip(map, availableGames, availableMapFolderOrZipNames, mapNamePropertyList);
			}
		}
		System.out.println("Finished parsing all available game xmls. ");
	}
	
	private static List<File> allMapFiles()
	{
		final List<File> rVal = new ArrayList<File>();
		// prioritize user maps folder over root folder
		rVal.addAll(safeListFiles(GameRunner2.getUserMapsFolder()));
		rVal.addAll(safeListFiles(NewGameChooserModel.getDefaultMapsDir()));
		return rVal;
	}
	
	private static List<File> safeListFiles(final File f)
	{
		final File[] files = f.listFiles();
		if (files == null)
		{
			return Collections.emptyList();
		}
		return Arrays.asList(files);
	}
	
	private static void populateFromDirectory(final File mapDir, final Map<String, URI> availableGames, final Set<String> availableMapFolderOrZipNames, final Set<String> mapNamePropertyList)
	{
		final File games = new File(mapDir, "games");
		if (!games.exists())
		{
			return;// no games in this map dir
		}
		for (final File game : games.listFiles())
		{
			if (game.isFile() && game.getName().toLowerCase().endsWith("xml"))
			{
				final boolean added = addToAvailableGames(game.toURI(), availableGames, mapNamePropertyList);
				if (added)
					availableMapFolderOrZipNames.add(mapDir.getName());
			}
		}
	}
	
	private static void populateFromZip(final File map, final Map<String, URI> availableGames, final Set<String> availableMapFolderOrZipNames, final Set<String> mapNamePropertyList)
	{
		try
		{
			final FileInputStream fis = new FileInputStream(map);
			try
			{
				final ZipInputStream zis = new ZipInputStream(fis);
				try
				{
					ZipEntry entry = zis.getNextEntry();
					while (entry != null)
					{
						if (entry.getName().startsWith("games/") && entry.getName().toLowerCase().endsWith(".xml"))
						{
							final URLClassLoader loader = new URLClassLoader(new URL[] { map.toURI().toURL() });
							final URL url = loader.getResource(entry.getName());
							// we have to close the loader to allow files to be deleted on windows
							ClassLoaderUtil.closeLoader(loader);
							try
							{
								final boolean added = addToAvailableGames(new URI(url.toString().replace(" ", "%20")), availableGames, mapNamePropertyList);
								if (added && map.getName().length() > 4)
									availableMapFolderOrZipNames.add(map.getName().substring(0, map.getName().length() - ZIP_EXTENSION.length()));
							} catch (final URISyntaxException e)
							{
								// only happens when URI couldn't be build and therefore no entry was added. That's fine
							}
						}
						zis.closeEntry();
						entry = zis.getNextEntry();
					}
				} finally
				{
					zis.close();
				}
			} finally
			{
				fis.close();
			}
		} catch (final IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	public static boolean addToAvailableGames(final URI uri, final Map<String, URI> availableGames, final Set<String> mapNamePropertyList)
	{
		if (uri == null)
			return false;
		InputStream input = null;
		try
		{
			input = uri.toURL().openStream();
			try
			{
				final GameData data = new GameParser().parse(input, s_delayedParsing);
				final String name = data.getGameName();
				final String mapName = data.getProperties().get(Constants.MAP_NAME, "");
				if (!availableGames.containsKey(name))
				{
					availableGames.put(name, uri);
					if (mapName.length() > 0)
						mapNamePropertyList.add(mapName);
					return true;
				}
			} catch (final Exception e2)
			{// ignore
			}
		} catch (final Exception e1)
		{// ignore
		} finally
		{
			try
			{
				if (input != null)
					input.close();
			} catch (final IOException e3)
			{// ignore
			}
		}
		return false;
	}
	
	public static String getGameXMLLocation(final URI uri)
	{
		if (uri == null)
			return null;
		final String raw = uri.toString();
		final String base = GameRunner2.getRootFolder().toURI().toString() + "maps";
		if (raw.startsWith(base))
		{
			return raw.substring(base.length());
		}
		if (raw.startsWith("jar:" + base))
		{
			return raw.substring("jar:".length() + base.length());
		}
		return raw;
	}
	
	public static GameData getGameDataFromXML(final URI uri)
	{
		if (uri == null)
			return null;
		GameData data = null;
		InputStream input = null;
		boolean error = false;
		try
		{
			input = uri.toURL().openStream();
			try
			{
				data = new GameParser().parse(input, false);
			} catch (final Exception e)
			{
				error = true;
			}
		} catch (final Exception e1)
		{
			error = true;
		} finally
		{
			try
			{
				if (input != null)
					input.close();
			} catch (final IOException e2)
			{// ignore
			}
		}
		if (error)
			return null;
		return data;
	}
}


class HeadlessGameSelectorPanel extends JPanel implements Observer
{
	private static final long serialVersionUID = 8104336314898207108L;
	private JLabel m_nameText;
	private JLabel m_versionText;
	private JLabel m_fileNameLabel;
	private JLabel m_fileNameText;
	private JLabel m_nameLabel;
	private JLabel m_versionLabel;
	private JLabel m_roundLabel;
	private JLabel m_roundText;
	private JButton m_loadSavedGame;
	private JButton m_loadNewGame;
	private JButton m_gameOptions;
	private final GameSelectorModel m_model;
	// private final IGamePropertiesCache m_gamePropertiesCache = new FileBackedGamePropertiesCache();
	private final Map<String, Object> m_originalPropertiesMap = new HashMap<String, Object>();
	private final AvailableGames m_availableGames;
	
	public HeadlessGameSelectorPanel(final GameSelectorModel model, final AvailableGames availableGames)
	{
		m_availableGames = availableGames;
		m_model = model;
		m_model.addObserver(this);
		/*final GameData data = model.getGameData();
		if (data != null)
		{
			setOriginalPropertiesMap(data);
			m_gamePropertiesCache.loadCachedGamePropertiesInto(data);
		}*/
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
		updateGameData();
	}
	
	private void updateGameData()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					updateGameData();
				}
			});
			return;
		}
		m_nameText.setText(m_model.getGameName());
		m_versionText.setText(m_model.getGameVersion());
		m_roundText.setText(m_model.getGameRound());
		String fileName = m_model.getFileName();
		if (fileName != null && fileName.length() > 1)
		{
			try
			{
				fileName = URLDecoder.decode(fileName, "UTF-8");
			} catch (final IllegalArgumentException e)
			{// ignore
			} catch (final UnsupportedEncodingException e)
			{// ignore
			}
		}
		m_fileNameText.setText(getLimitedFileNameText(fileName));
		m_fileNameText.setToolTipText(fileName);
	}
	
	private String getLimitedFileNameText(final String fileName)
	{
		final int maxLength = 25;
		if (fileName.length() <= maxLength)
		{
			return fileName;
		}
		int cuttoff = 18;
		// /games will be in most paths,
		// try to ignore it
		if (fileName.indexOf("games") > 0)
		{
			cuttoff = Math.min(18, fileName.indexOf("games"));
		}
		final int length = fileName.length();
		return fileName.substring(0, cuttoff) + "..." + fileName.substring(length - (maxLength - cuttoff) - 2, length);
	}
	
	private void createComponents()
	{
		m_nameLabel = new JLabel("Game Name:");
		m_versionLabel = new JLabel("Game Version:");
		m_roundLabel = new JLabel("Game Round:");
		m_fileNameLabel = new JLabel("File Name:");
		m_nameText = new JLabel();
		m_versionText = new JLabel();
		m_roundText = new JLabel();
		m_fileNameText = new JLabel();
		m_loadNewGame = new JButton("Choose Game...");
		m_loadSavedGame = new JButton("Load Saved Game...");
		m_gameOptions = new JButton("Game Options...");
	}
	
	private void layoutComponents()
	{
		setLayout(new GridBagLayout());
		add(m_nameLabel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 3, 5), 0, 0));
		add(m_nameText, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 0, 3, 0), 0, 0));
		add(m_versionLabel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 3, 5), 0, 0));
		add(m_versionText, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 3, 0), 0, 0));
		add(m_roundLabel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 3, 5), 0, 0));
		add(m_roundText, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 3, 0), 0, 0));
		add(m_fileNameLabel, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 10, 3, 5), 0, 0));
		add(m_fileNameText, new GridBagConstraints(0, 4, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 3, 5), 0, 0));
		add(m_loadNewGame, new GridBagConstraints(0, 5, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(25, 10, 10, 10), 0, 0));
		add(m_loadSavedGame, new GridBagConstraints(0, 6, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 10, 10), 0, 0));
		add(m_gameOptions, new GridBagConstraints(0, 7, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(25, 10, 10, 10), 0, 0));
		// spacer
		add(new JPanel(), new GridBagConstraints(0, 8, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
	}
	
	private void setupListeners()
	{
		m_loadNewGame.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				selectGameFile(false);
			}
		});
		m_loadSavedGame.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				selectGameFile(true);
			}
		});
		m_gameOptions.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				selectGameOptions();
			}
		});
	}
	
	/*
	private void setOriginalPropertiesMap(final GameData data)
	{
		m_originalPropertiesMap.clear();
		if (data != null)
		{
			for (final IEditableProperty property : data.getProperties().getEditableProperties())
			{
				m_originalPropertiesMap.put(property.getName(), property.getValue());
			}
		}
	}*/
	
	private void selectGameOptions()
	{
		// backup current game properties before showing dialog
		final Map<String, Object> currentPropertiesMap = new HashMap<String, Object>();
		for (final IEditableProperty property : m_model.getGameData().getProperties().getEditableProperties())
		{
			currentPropertiesMap.put(property.getName(), property.getValue());
		}
		
		final PropertiesUI panel = new PropertiesUI(m_model.getGameData().getProperties(), true);
		final JScrollPane scroll = new JScrollPane(panel);
		scroll.setBorder(null);
		scroll.getViewport().setBorder(null);
		
		final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
		final String ok = "OK";
		final String cancel = "Cancel";
		// final String makeDefault = "Make Default";
		final String reset = "Reset";
		pane.setOptions(new Object[] { ok, /*makeDefault,*/reset, cancel });
		final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(this), "Game Options");
		window.setVisible(true);
		
		final Object buttonPressed = pane.getValue();
		if (buttonPressed == null || buttonPressed.equals(cancel))
		{
			// restore properties, if cancel was pressed, or window was closed
			final Iterator<IEditableProperty> itr = m_model.getGameData().getProperties().getEditableProperties().iterator();
			while (itr.hasNext())
			{
				final IEditableProperty property = itr.next();
				property.setValue(currentPropertiesMap.get(property.getName()));
			}
		}
		else if (buttonPressed.equals(reset))
		{
			if (!m_originalPropertiesMap.isEmpty())
			{
				// restore properties, if cancel was pressed, or window was closed
				final Iterator<IEditableProperty> itr = m_model.getGameData().getProperties().getEditableProperties().iterator();
				while (itr.hasNext())
				{
					final IEditableProperty property = itr.next();
					property.setValue(m_originalPropertiesMap.get(property.getName()));
				}
				selectGameOptions();
				return;
			}
		}
		/*else if (buttonPressed.equals(makeDefault))
		{
			m_gamePropertiesCache.cacheGameProperties(m_model.getGameData());
		}*/
		else
		{
			// ok was clicked, and we have modified the properties already
		}
	}
	
	private void setWidgetActivation()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setWidgetActivation();
				}
			});
			return;
		}
		final boolean canSelectGameData = m_model != null && m_model.canSelect();
		m_loadSavedGame.setEnabled(canSelectGameData);
		m_loadNewGame.setEnabled(canSelectGameData);
		// Disable game options if there are none.
		if (canSelectGameData && m_model.getGameData() != null && m_model.getGameData().getProperties().getEditableProperties().size() > 0)
			m_gameOptions.setEnabled(true);
		else
			m_gameOptions.setEnabled(false);
		// we don't want them starting new games if we are an old jar
		if (GameRunner2.areWeOldExtraJar())
		{
			m_loadNewGame.setEnabled(false);
			// m_loadSavedGame.setEnabled(false);
			m_loadNewGame.setToolTipText("This is disabled on older engine jars, please start new games with the latest version of TripleA.");
			// m_loadSavedGame.setToolTipText("This is disabled on older engine jars, please open savegames from the latest version of TripleA.");
		}
	}
	
	public void update(final Observable o, final Object arg)
	{
		updateGameData();
		setWidgetActivation();
	}
	
	private void selectGameFile(final boolean saved)
	{
		// For some strange reason,
		// the only way to get a Mac OS X native-style file dialog
		// is to use an AWT FileDialog instead of a Swing JDialog
		if (saved)
		{
			if (GameRunner.isMac())
			{
				final FileDialog fileDialog = new FileDialog(MainFrame.getInstance());
				fileDialog.setMode(FileDialog.LOAD);
				SaveGameFileChooser.ensureDefaultDirExists();
				fileDialog.setDirectory(SaveGameFileChooser.DEFAULT_DIRECTORY.getPath());
				fileDialog.setFilenameFilter(new FilenameFilter()
				{
					public boolean accept(final File dir, final String name)
					{
						// the extension should be .tsvg, but find svg extensions as well
						// also, macs download the file as tsvg.gz, so accept that as well
						return name.endsWith(".tsvg") || name.endsWith(".svg") || name.endsWith("tsvg.gz");
					}
				});
				fileDialog.setVisible(true);
				final String fileName = fileDialog.getFile();
				final String dirName = fileDialog.getDirectory();
				if (fileName == null)
					return;
				else
				{
					final File f = new File(dirName, fileName);
					m_model.load(f, this);
					// setOriginalPropertiesMap(m_model.getGameData());
				}
			}
			// Non-Mac platforms should use the normal Swing JFileChooser
			else
			{
				final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
				final int rVal = fileChooser.showOpenDialog(JOptionPane.getFrameForComponent(this));
				if (rVal != JFileChooser.APPROVE_OPTION)
					return;
				m_model.load(fileChooser.getSelectedFile(), this);
				// setOriginalPropertiesMap(m_model.getGameData());
			}
		}
		else
		{
			final Vector<String> games = new Vector<String>(m_availableGames.getGameNames());
			@SuppressWarnings({ "rawtypes", "unchecked" })
			final JList list = new JList(games);
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.setVisibleRowCount(20);
			final JScrollPane listScroll = new JScrollPane(list);
			final int option = JOptionPane.showConfirmDialog(null, listScroll, "Choose Game", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (option == JOptionPane.OK_OPTION)
			{
				final String gameSelected = (String) list.getSelectedValue();
				m_model.load(m_availableGames.getGameData(gameSelected), m_availableGames.getGameFilePath(gameSelected));
			}
		}
	}
}


class HeadlessGameServerConsole
{
	private final HeadlessGameServer server;
	private final PrintStream out;
	private final BufferedReader in;
	@SuppressWarnings("deprecation")
	private final String startDate = new Date().toGMTString();
	private boolean m_shutDown = false;
	private boolean m_chatMode = false;
	
	public HeadlessGameServerConsole(final HeadlessGameServer server, final InputStream in, final PrintStream out)
	{
		this.out = out;
		this.in = new BufferedReader(new InputStreamReader(in));
		this.server = server;
	}
	
	public void start()
	{
		final Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				printEvalLoop();
			}
		}, "Headless console eval print loop");
		t.setDaemon(true);
		t.start();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{
			public void run()
			{
				// m_shutDown = true;
				out.println("Shutting Down.   See log file.");
			}
		}));
	}
	
	private void printEvalLoop()
	{
		out.println();
		while (!m_shutDown)
		{
			out.print(">>>>");
			out.flush();
			try
			{
				final String command = in.readLine();
				// if (m_shutDown)
				// break;
				if (command != null)
					process(command.trim());
			} catch (final Throwable t)
			{
				t.printStackTrace();
				t.printStackTrace(out);
			}
		}
	}
	
	private void process(final String command)
	{
		if (command.equals(""))
		{
			return;
		}
		final String noun = command.split("\\s")[0];
		if (noun.equalsIgnoreCase("help"))
		{
			showHelp();
		}
		else if (noun.equalsIgnoreCase("status"))
		{
			showStatus();
		}
		else if (noun.equalsIgnoreCase("save"))
		{
			save(command);
		}
		else if (noun.equalsIgnoreCase("stop"))
		{
			stop();
		}
		else if (noun.equalsIgnoreCase("quit"))
		{
			quit();
		}
		else if (noun.equalsIgnoreCase("connections"))
		{
			showConnections();
		}
		else if (noun.equalsIgnoreCase("send"))
		{
			send(command);
		}
		else if (noun.equalsIgnoreCase("chatlog"))
		{
			chatlog();
		}
		else if (noun.equalsIgnoreCase("chatmode"))
		{
			chatmode();
		}
		else if (noun.equalsIgnoreCase("mute"))
		{
			mute(command);
		}
		else if (noun.equalsIgnoreCase("boot"))
		{
			boot(command);
		}
		else if (noun.equalsIgnoreCase("ban"))
		{
			ban(command);
		}
		else if (noun.equalsIgnoreCase("memory"))
		{
			memory();
		}
		else if (noun.equalsIgnoreCase("threads"))
		{
			threads();
		}
		else if (noun.equalsIgnoreCase("dump"))
		{
			dump();
		}
		else
		{
			out.println("Unrecognized command:" + command);
			showHelp();
		}
	}
	
	private void send(final String command)
	{
		if (server == null || command == null)
			return;
		final Chat chat = server.getChat();
		if (chat == null)
			return;
		try
		{
			final String message;
			if (command.length() > 5)
			{
				message = command.substring(5, command.length());
			}
			else
			{
				out.println("Input chat message: ");
				message = in.readLine();
			}
			chat.sendMessage(message, false);
		} catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void chatlog()
	{
		if (server == null)
			return;
		final IChatPanel chat = server.getServerModel().getChatPanel();
		if (chat == null)
			return;
		out.println();
		out.println(chat.getAllText());
		out.println();
	}
	
	private void chatmode()
	{
		if (server == null)
			return;
		final IChatPanel chat = server.getServerModel().getChatPanel();
		if (chat == null || !(chat instanceof HeadlessChat))
			return;
		m_chatMode = !m_chatMode;
		out.println("chatmode is now " + (m_chatMode ? "on" : "off"));
		final HeadlessChat headlessChat = (HeadlessChat) chat;
		headlessChat.setPrintStream(m_chatMode ? out : null);
	}
	
	private void dump()
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("Dump to Log:");
		sb.append("\n\nStatus:\n");
		sb.append(getStatus());
		sb.append("\n\nConnections:\n");
		sb.append(getConnections());
		sb.append("\n\n");
		sb.append(DebugUtils.getThreadDumps());
		sb.append("\n\n");
		sb.append(DebugUtils.getMemory());
		sb.append("\n\nDump finished.\n");
		HeadlessGameServer.log(sb.toString());
	}
	
	private void threads()
	{
		out.println(DebugUtils.getThreadDumps());
	}
	
	private void memory()
	{
		out.println(DebugUtils.getMemory());
	}
	
	public void println(final String string)
	{
		out.println(string);
	}
	
	private void mute(final String command)
	{
		if (server == null || server.getServerModel() == null)
			return;
		final IServerMessenger messenger = server.getServerModel().getMessenger();
		if (messenger == null)
			return;
		final Set<INode> nodes = server.getServerModel().getMessenger().getNodes();
		if (nodes == null)
			return;
		try
		{
			final String name;
			if (command.length() > 4 && command.split(" ").length > 1)
			{
				name = command.split(" ")[1];
			}
			else
			{
				out.println("Input player name to mute: ");
				name = in.readLine();
			}
			if (name == null || name.length() < 1)
			{
				out.println("Invalid name");
				return;
			}
			final String minutes;
			if (command.length() > 4 && command.split(" ").length > 2)
			{
				minutes = command.split(" ")[2];
			}
			else
			{
				out.println("Input minutes to mute: ");
				minutes = in.readLine();
			}
			final long min;
			try
			{
				min = Math.max(0, Math.min(60 * 24 * 2, Long.parseLong(minutes))); // max out at 48 hours
			} catch (final NumberFormatException nfe)
			{
				out.println("Invalid minutes");
				return;
			}
			final long expire = System.currentTimeMillis() + (min * 1000 * 60); // milliseconds
			for (final INode node : nodes)
			{
				final String realName = node.getName().split(" ")[0];
				final String ip = node.getAddress().getHostAddress();
				final String mac = messenger.GetPlayerMac(node.getName());
				if (realName.equals(name))
				{
					messenger.NotifyUsernameMutingOfPlayer(realName, new Date(expire));
					messenger.NotifyIPMutingOfPlayer(ip, new Date(expire));
					messenger.NotifyMacMutingOfPlayer(mac, new Date(expire));
					return;
				}
			}
		} catch (final IOException e)
		{
			e.printStackTrace();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void boot(final String command)
	{
		if (server == null || server.getServerModel() == null)
			return;
		final IServerMessenger messenger = server.getServerModel().getMessenger();
		if (messenger == null)
			return;
		final Set<INode> nodes = server.getServerModel().getMessenger().getNodes();
		if (nodes == null)
			return;
		try
		{
			final String name;
			if (command.length() > 4 && command.split(" ").length > 1)
			{
				name = command.split(" ")[1];
			}
			else
			{
				out.println("Input player name to boot: ");
				name = in.readLine();
			}
			if (name == null || name.length() < 1)
			{
				out.println("Invalid name");
				return;
			}
			for (final INode node : nodes)
			{
				final String realName = node.getName().split(" ")[0];
				if (realName.equals(name))
				{
					messenger.removeConnection(node);
				}
			}
		} catch (final IOException e)
		{
			e.printStackTrace();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void ban(final String command)
	{
		if (server == null || server.getServerModel() == null)
			return;
		final IServerMessenger messenger = server.getServerModel().getMessenger();
		if (messenger == null)
			return;
		final Set<INode> nodes = server.getServerModel().getMessenger().getNodes();
		if (nodes == null)
			return;
		try
		{
			final String name;
			if (command.length() > 4 && command.split(" ").length > 1)
			{
				name = command.split(" ")[1];
			}
			else
			{
				out.println("Input player name to ban: ");
				name = in.readLine();
			}
			if (name == null || name.length() < 1)
			{
				out.println("Invalid name");
				return;
			}
			final String hours;
			if (command.length() > 4 && command.split(" ").length > 2)
			{
				hours = command.split(" ")[2];
			}
			else
			{
				out.println("Input hours to ban: ");
				hours = in.readLine();
			}
			final long hrs;
			try
			{
				hrs = Math.max(0, Math.min(24 * 30, Long.parseLong(hours))); // max out at 30 days
			} catch (final NumberFormatException nfe)
			{
				out.println("Invalid minutes");
				return;
			}
			final long expire = System.currentTimeMillis() + (hrs * 1000 * 60 * 60); // milliseconds
			for (final INode node : nodes)
			{
				final String realName = node.getName().split(" ")[0];
				final String ip = node.getAddress().getHostAddress();
				final String mac = messenger.GetPlayerMac(node.getName());
				if (realName.equals(name))
				{
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
		} catch (final IOException e)
		{
			e.printStackTrace();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void save(final String command)
	{
		final ServerGame game = server.getIGame();
		if (game == null)
		{
			out.println("No Game Currently Running");
			return;
		}
		else
		{
			try
			{
				String saveName;
				if (command.length() > 5)
				{
					saveName = command.substring(5, command.length());
				}
				else
				{
					out.println("Input savegame filename: ");
					saveName = in.readLine();
				}
				if (saveName == null || saveName.length() < 2)
				{
					out.println("Invalid save name");
					return;
				}
				if (!saveName.endsWith(".tsvg"))
					saveName += ".tsvg";
				SaveGameFileChooser.ensureDefaultDirExists();
				final File f = new File(SaveGameFileChooser.DEFAULT_DIRECTORY, saveName);
				try
				{
					game.saveGame(f);
				} catch (final Exception e)
				{
					e.printStackTrace();
				}
			} catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void stop()
	{
		final ServerGame game = server.getIGame();
		if (game == null)
		{
			out.println("No Game Currently Running");
			return;
		}
		out.println("Are you sure? (y/f/n) [f = yes + force stop]");
		try
		{
			final String readin = in.readLine();
			if (readin == null)
				return;
			final boolean stop = readin.toLowerCase().startsWith("y");
			final boolean forceStop = readin.toLowerCase().startsWith("f");
			if (stop || forceStop)
			{
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
					game.saveGame(f);
				} catch (final Exception e)
				{
					e.printStackTrace();
				}
				game.stopGame(forceStop);
			}
		} catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void quit()
	{
		out.println("Are you sure? (y/n)");
		try
		{
			final String readin = in.readLine();
			if (readin != null && readin.toLowerCase().startsWith("y"))
			{
				m_shutDown = true;
				System.exit(0);
			}
		} catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void showConnections()
	{
		out.println(getConnections());
	}
	
	private String getConnections()
	{
		final StringBuilder sb = new StringBuilder();
		if (server != null && server.getServerModel() != null && server.getServerModel().getMessenger() != null)
		{
			sb.append("Connected: " + server.getServerModel().getMessenger().isConnected() + "\n" + "Nodes: \n");
			final Set<INode> nodes = server.getServerModel().getMessenger().getNodes();
			if (nodes == null)
				sb.append("  null\n");
			else
			{
				for (final INode node : nodes)
				{
					sb.append("  " + node + "\n");
				}
			}
		}
		else
			sb.append("Not Connected to Anything");
		return sb.toString();
	}
	
	private void showStatus()
	{
		out.println(getStatus());
	}
	
	private String getStatus()
	{
		String message = "Server Start Date: " + startDate;
		if (server != null)
		{
			final ServerGame game = server.getIGame();
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
		}
		return message;
	}
	
	private void showHelp()
	{
		out.println("Available commands:\n"
					+ "  help - show this message\n"
					+ "  status - show status information\n"
					+ "  dump - prints threads, memory, status, connections, to the log file\n"
					+ "  connections - show all connected players\n"
					+ "  mute - mute player\n"
					+ "  boot - boot player\n"
					+ "  ban - ban player\n"
					+ "  send - sends a chat message\n"
					+ "  chatmode - toggles the showing of chat messages as they come in\n"
					+ "  chatlog - shows the chat log\n"
					+ "  memory - show memory usage\n"
					+ "  threads - get thread dumps\n"
					+ "  save - saves game to filename\n"
					+ "  stop - saves then stops current game and goes back to waiting\n"
					+ "  quit - quit\n");
	}
}
