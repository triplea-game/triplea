package games.strategy.engine.framework;

import games.strategy.debug.Console;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
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
import games.strategy.sound.ClipPlayer;
import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.util.ClassLoaderUtil;

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
import java.util.logging.LogManager;
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
	public static final String TRIPLEA_GAME_HOST_UI_PROPERTY = "triplea.game.host.ui";
	public static final String TRIPLEA_HEADLESS = "triplea.headless";
	public static final String TRIPLEA_GAME_HOST_CONSOLE_PROPERTY = "triplea.game.host.console";
	private static HeadlessGameServer s_instance = null;
	private final AvailableGames m_availableGames;
	private final GameSelectorModel m_gameSelectorModel;
	private SetupPanelModel m_setupPanelModel = null;
	private HeadlessServerMainPanel m_mainPanel = null;
	private final boolean m_useUI;
	private final ScheduledExecutorService m_lobbyWatcherResetupThread = Executors.newScheduledThreadPool(1);
	private ServerGame m_iGame = null;
	private boolean m_shutDown = false;
	
	public static String[] getProperties()
	{
		return new String[] { GameRunner2.TRIPLEA_GAME_PROPERTY, TRIPLEA_GAME_HOST_CONSOLE_PROPERTY, TRIPLEA_GAME_HOST_UI_PROPERTY, GameRunner2.TRIPLEA_SERVER_PROPERTY,
					GameRunner2.TRIPLEA_PORT_PROPERTY, GameRunner2.TRIPLEA_NAME_PROPERTY, GameRunner2.LOBBY_HOST, GameRunner2.LOBBY_PORT, GameRunner2.LOBBY_GAME_COMMENTS,
					GameRunner2.LOBBY_GAME_HOSTED_BY, GameRunner2.TRIPLEA_SERVER_PASSWORD_PROPERTY };
	}
	
	private static void usage()
	{
		System.out.println("Arguments\n"
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
					+ "   " + GameRunner2.TRIPLEA_SERVER_PASSWORD_PROPERTY + "=<password>\n"
					+ "\n"
					+ "   If there is only one argument, and it does not start with a prefix, the argument will be \n"
					+ "   taken as the name of the file to load.\n"
					+ "\n"
					+ "   Examples:\n"
					+ "   To start a game using the given file:\n"
					+ "\n"
					+ "   triplea /home/sgb/games/test.xml\n"
					+ "\n"
					+ "   or\n"
					+ "\n"
					+ "   triplea triplea.game=/home/sgb/games/test.xml\n"
					+ "\n"
					+ "   To start a server with the given game\n"
					+ "\n"
					+ "   triplea triplea.game=/home/sgb/games/test.xml triplea.port=3300 triplea.name=Allan"
					+ "\n"
					+ "   To start a server, you can optionally password protect the game using triplea.server.password=foo");
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
		if (m_setupPanelModel.getPanel() != null)
		{
			m_gameSelectorModel.load(m_availableGames.getGameData(gameName), m_availableGames.getGameFilePath(gameName));
		}
	}
	
	public synchronized void loadGameSave(final File file)
	{
		// don't change mid-game
		if (m_setupPanelModel.getPanel() != null)
		{
			m_gameSelectorModel.load(file, null);
		}
	}
	
	public synchronized void loadGameSave(final InputStream input, final String fileName)
	{
		// don't change mid-game
		if (m_setupPanelModel.getPanel() != null)
		{
			m_gameSelectorModel.load(input, fileName);
		}
	}
	
	public synchronized void loadGameSave(final ObjectInputStream input, final String fileName)
	{
		// don't change mid-game
		if (m_setupPanelModel.getPanel() != null)
		{
			m_gameSelectorModel.load(input, fileName);
		}
	}
	
	public static synchronized void setServerGame(final ServerGame serverGame)
	{
		final HeadlessGameServer instance = getInstance();
		if (instance != null)
			instance.m_iGame = serverGame;
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
				System.out.println("Running shutdown script");
				shutdown();
			}
		}));
		m_useUI = useUI;
		m_availableGames = new AvailableGames();
		m_gameSelectorModel = new GameSelectorModel();
		final String fileName = System.getProperty(GameRunner2.TRIPLEA_GAME_PROPERTY, "");
		if (fileName.length() > 0)
		{
			final File file = new File(fileName);
			m_gameSelectorModel.load(file, null);
		}
		if (m_useUI)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
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
					m_setupPanelModel = new HeadlessServerSetupPanelModel(m_gameSelectorModel, null);
					m_setupPanelModel.showSelectType();
					System.out.println("Waiting for users to connect.");
					waitForUsersHeadless();
				}
			};
			final Thread t = new Thread(r, "Initialize Headless Server Setup Model");
			t.start();
		}
		m_lobbyWatcherResetupThread.scheduleAtFixedRate(new Runnable()
		{
			public void run()
			{
				try
				{
					final ISetupPanel setup = m_setupPanelModel.getPanel();
					if (setup == null)
						return;
					if (setup instanceof ServerSetupPanel)
					{
						System.out.println("Restarting lobby watcher");
						((ServerSetupPanel) setup).shutDownLobbyWatcher();
						try
						{
							Thread.sleep(1000);
						} catch (final InterruptedException e)
						{
						}
						resetLobbyHostOldExtensionProperties();
						((ServerSetupPanel) setup).createLobbyWatcher();
					}
					else if (setup instanceof HeadlessServerSetup)
					{
						System.out.println("Restarting lobby watcher");
						((HeadlessServerSetup) setup).shutDownLobbyWatcher();
						try
						{
							Thread.sleep(1000);
						} catch (final InterruptedException e)
						{
						}
						resetLobbyHostOldExtensionProperties();
						((HeadlessServerSetup) setup).createLobbyWatcher();
					}
				} catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
		}, 28800, 28800, TimeUnit.SECONDS);
	}
	
	private void resetLobbyHostOldExtensionProperties()
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
				Thread.sleep(250);
			}
		} catch (final Exception e)
		{
		}
		try
		{
			if (m_iGame != null)
			{
				m_iGame.stopGame();
				Thread.sleep(500);
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
					((ServerSetupPanel) setup).cancel();
				}
				else if (setup != null && setup instanceof HeadlessServerSetup)
				{
					((HeadlessServerSetup) setup).cancel();
				}
				Thread.sleep(250);
			}
		} catch (final Exception e)
		{
		}
		try
		{
			if (m_gameSelectorModel != null && m_gameSelectorModel.getGameData() != null)
			{
				m_gameSelectorModel.getGameData().clearAllListeners();
				Thread.sleep(250);
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
						Thread.sleep(5000);
					} catch (final InterruptedException e)
					{
					}
					if (m_setupPanelModel != null && m_setupPanelModel.getPanel() != null && m_setupPanelModel.getPanel().canGameStart())
					{
						System.out.println("Starting Game.");
						m_setupPanelModel.getPanel().preStartGame();
						m_setupPanelModel.getPanel().getLauncher().launch(null);
						m_setupPanelModel.getPanel().postStartGame();
						break; // TODO: need a latch instead?
					}
				}
			}
		};
		final Thread t = new Thread(r, "Headless Server Waiting For Users To Connect And Start");
		t.start();
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
		else
		{
			return null;
		}
	}
	
	public static void main(final String[] args)
	{
		setupLogging();
		handleCommandLineArgs(args);
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
			final InputStream in = System.in;
			final PrintStream out = System.out;
			startConsole(server, in, out);
		}
	}
	
	private static void startConsole(final HeadlessGameServer server, final InputStream in, final PrintStream out)
	{
		System.out.println("Starting console.");
		final HeadlessGameServerConsole thread = new HeadlessGameServerConsole(server, in, out);
		thread.start();
	}
	
	public static void setupLogging()
	{
		// setup logging to read our logging.properties
		try
		{
			LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
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
		
		boolean usagePrinted = false;
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
				System.out.println("Unrecogized:" + args[argIndex]);
				if (!usagePrinted)
				{
					usagePrinted = true;
					usage();
				}
			}
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
	private InGameLobbyWatcher m_lobbyWatcher;
	
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
		m_lobbyWatcher = InGameLobbyWatcher.newInGameLobbyWatcher(m_model.getMessenger(), null);
		if (m_lobbyWatcher != null)
		{
			m_lobbyWatcher.setGameSelectorModel(m_gameSelectorModel);
		}
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
	
	public ChatPanel getChatPanel()
	{
		return m_model.getChatPanel();
	}
	
	public ILauncher getLauncher()
	{
		final ServerLauncher launcher = (ServerLauncher) m_model.getLauncher();
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
		if (m_gameTypePanelModel != null && m_gameTypePanelModel.getPanel() != null && m_gameTypePanelModel.getPanel().getChatPanel() != null)
		{
			chat = m_gameTypePanelModel.getPanel().getChatPanel();
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
					m_gameSetupPanel.cancel();
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
		System.out.println("Starting Game.");
		m_gameSetupPanel.preStartGame();
		m_gameTypePanelModel.getPanel().getLauncher().launch(this);
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
	private final TreeMap<String, URI> m_availableGames = new TreeMap<String, URI>();
	
	public AvailableGames()
	{
		populateAvailableGames(m_availableGames);
	}
	
	public List<String> getGameNames()
	{
		return new ArrayList<String>(m_availableGames.keySet());
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
	
	private static void populateAvailableGames(final Map<String, URI> availableGames)
	{
		System.out.println("Parsing all available games (this could take a while). ");
		for (final File map : allMapFiles())
		{
			if (map.isDirectory())
			{
				populateFromDirectory(map, availableGames);
			}
			else if (map.isFile() && map.getName().toLowerCase().endsWith(".zip"))
			{
				populateFromZip(map, availableGames);
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
	
	private static void populateFromDirectory(final File mapDir, final Map<String, URI> availableGames)
	{
		final File games = new File(mapDir, "games");
		if (!games.exists())
		{
			return;// no games in this map dir
		}
		for (final File game : games.listFiles())
		{
			if (game.isFile() && game.getName().toLowerCase().endsWith("xml"))
				addToAvailableGames(game.toURI(), availableGames);
		}
	}
	
	private static void populateFromZip(final File map, final Map<String, URI> availableGames)
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
								addToAvailableGames(new URI(url.toString().replace(" ", "%20")), availableGames);
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
	
	public static void addToAvailableGames(final URI uri, final Map<String, URI> availableGames)
	{
		if (uri == null)
			return;
		InputStream input;
		try
		{
			input = uri.toURL().openStream();
			try
			{
				final GameData data = new GameParser().parse(input, s_delayedParsing);
				final String name = data.getGameName();
				if (!availableGames.containsKey(name))
					availableGames.put(name, uri);
			} catch (final Exception e2)
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
		} catch (final Exception e1)
		{// ignore
		}
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
		InputStream input;
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
			} finally
			{
				try
				{
					input.close();
				} catch (final IOException e2)
				{// ignore
				}
			}
		} catch (final Exception e1)
		{
			error = true;
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
		else if (noun.equalsIgnoreCase("quit"))
		{
			quit();
		}
		else if (noun.equalsIgnoreCase("memory"))
		{
			memory();
		}
		else if (noun.equalsIgnoreCase("threads"))
		{
			threads();
		}
		else
		{
			out.println("Unrecognized command:" + command);
			showHelp();
		}
	}
	
	private void threads()
	{
		out.println(Console.getThreadDumps());
	}
	
	private void memory()
	{
		out.println(Console.getMemory());
	}
	
	private void quit()
	{
		out.println("Are you sure? (y/n)");
		try
		{
			if (in.readLine().toLowerCase().startsWith("y"))
			{
				m_shutDown = true;
				System.exit(0);
			}
		} catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void showStatus()
	{
		String message = "Server Start Date: " + startDate;
		if (server != null)
		{
			final ServerGame game = server.getIGame();
			if (game != null)
			{
				message += "\nIs currently running: " + game.isGameSequenceRunning() + "\nIs GameOver: " + game.isGameOver()
							+ "\nGame: " + game.getData().getGameName() + "\nRound: " + game.getData().getSequence().getRound();
			}
			else
			{
				message += "\nCurrently Waiting To Start A Game";
			}
		}
		out.println(message);
	}
	
	private void showHelp()
	{
		out.println("Available commands:\n" + "  help - show this message\n" + "  memory - show memory usage\n" + "  status - show status information\n" + "  threads - get thread dumps\n"
					+ "  quit - quit\n");
	}
}
