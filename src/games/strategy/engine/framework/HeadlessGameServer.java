package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.ui.NewGameChooserModel;
import games.strategy.sound.ClipPlayer;
import games.strategy.util.ClassLoaderUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.LogManager;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Ideally a way of hosting a game, but headless.
 * 
 * @author veqryn (Mark Christopher Duncan)
 * 
 */
public class HeadlessGameServer
{
	public static final String TRIPLEA_GAME_HOST_UI_PROPERTY = "triplea.game.host.ui";
	public static final String TRIPLEA_GAME_HOST_CONSOLE_PROPERTY = "triplea.lobby.console";
	private static HeadlessGameServer s_instance = null;
	private final AvailableGames m_availableGames;
	private final GameSelectorModel m_gameSelectorModel;
	
	public static String[] getProperties()
	{
		return new String[] { GameRunner2.TRIPLEA_GAME_PROPERTY, TRIPLEA_GAME_HOST_UI_PROPERTY };
	}
	
	private static void usage()
	{
		System.out.println("Arguments\n");
	}
	
	public HeadlessGameServer()
	{
		super();
		if (s_instance != null)
			throw new IllegalStateException("Instance already exists");
		s_instance = this;
		m_availableGames = new AvailableGames();
		m_gameSelectorModel = new GameSelectorModel();
		final String fileName = System.getProperty(GameRunner2.TRIPLEA_GAME_PROPERTY, "");
		if (fileName.length() > 0)
		{
			final File file = new File(fileName);
			m_gameSelectorModel.load(file, null);
		}
		else
		{
			m_gameSelectorModel.load(m_availableGames.getGameData("Minimap"));
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final ServerModel model = new ServerModel(m_gameSelectorModel);
				if (!model.createServerMessenger(null))
				{
					model.cancel();
					return;
				}
				final ServerSetupPanel serverSetupPanel = new ServerSetupPanel(model, m_gameSelectorModel);
				final int option = JOptionPane.showConfirmDialog(null, serverSetupPanel, "Server Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
				if (option != JOptionPane.OK_OPTION || !serverSetupPanel.canGameStart())
				{
					model.cancel();
					return;
				}
				serverSetupPanel.preStartGame();
				serverSetupPanel.getLauncher().launch(new JFrame("Test Frame"));
				serverSetupPanel.postStartGame();
			}
		});
	}
	
	public static void main(final String[] args)
	{
		setupLogging();
		handleCommandLineArgs(args);
		final boolean startUI = Boolean.parseBoolean(System.getProperty(TRIPLEA_GAME_HOST_UI_PROPERTY, "false"));
		if (!startUI)
		{
			ClipPlayer.setBeSilent(true);
		}
		else
		{
			// startUI(server);
		}
		if (Boolean.parseBoolean(System.getProperty(TRIPLEA_GAME_HOST_CONSOLE_PROPERTY, "false")))
		{
			final InputStream in = System.in;
			final PrintStream out = System.out;
			// startConsole(server, in, out);
		}
		final HeadlessGameServer server = new HeadlessGameServer();
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


class AvailableGames
{
	private static final boolean s_delayedParsing = true;
	private final TreeMap<String, URI> m_availableGames = new TreeMap<String, URI>();
	
	public AvailableGames()
	{
		populateAvailableGames(m_availableGames);
	}
	
	public List<String> getGameNames()
	{
		return new ArrayList<String>(m_availableGames.navigableKeySet());
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
