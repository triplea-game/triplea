package games.strategy.engine.framework;

import games.strategy.debug.Console;
import games.strategy.engine.EngineVersion;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.background.WaitWindow;
import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.triplea.ui.TripleaMenu;

import java.util.List;
import java.util.logging.LogManager;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class GameRunner2
{
	public static final String LOOK_AND_FEEL_PREF = "LookAndFeel";
	public static final String DELAYED_PARSING = "DelayedParsing";
	public static final String TRIPLEA_GAME_PROPERTY = "triplea.game";
	public static final String TRIPLEA_HOST_PROPERTY = "triplea.host";
	public static final String TRIPLEA_PORT_PROPERTY = "triplea.port";
	public static final String TRIPLEA_SERVER_PASSWORD_PROPERTY = "triplea.server.password";
	public static final String TRIPLEA_CLIENT_PROPERTY = "triplea.client";
	public static final String TRIPLEA_SERVER_PROPERTY = "triplea.server";
	public static final String TRIPLEA_NAME_PROPERTY = "triplea.name";
	public static final String TRIPLEA_STARTED = "triplea.started";
	// these properties are for games that should connect to the Lobby Server
	public static final String LOBBY_PORT = "triplea.lobby.port";
	public static final String LOBBY_HOST = "triplea.lobby.host";
	public static final String LOBBY_GAME_COMMENTS = "triplea.lobby.game.comments";
	public static final String LOBBY_GAME_HOSTED_BY = "triplea.lobby.game.hostedBy";
	private static WaitWindow waitWindow;
	
	public static void main(final String[] args)
	{
		setupLogging();
		Console.getConsole().displayStandardError();
		Console.getConsole().displayStandardOutput();
		System.setProperty("sun.awt.exception.handler", ErrorHandler.class.getName());
		System.setProperty("triplea.engine.version", EngineVersion.VERSION.toString());
		setupLookAndFeel();
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					waitWindow = new WaitWindow("TripleA is starting...");
					waitWindow.setVisible(true);
					waitWindow.showWait();
				}
			});
		} catch (final Exception e)
		{
			// just don't show the wait window
		}
		handleCommandLineArgs(args);
		showMainFrame();
	}
	
	private static void showMainFrame()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final MainFrame frame = new MainFrame();
				frame.start();
				frame.requestFocus();
				frame.toFront();
				if (waitWindow != null)
					waitWindow.doneWait();
			}
		});
	}
	
	/**
	 * Move command line arguments to System.properties
	 */
	private static void handleCommandLineArgs(final String[] args)
	{
		final String[] properties = new String[] { TRIPLEA_SERVER_PROPERTY, TRIPLEA_CLIENT_PROPERTY, TRIPLEA_PORT_PROPERTY, TRIPLEA_HOST_PROPERTY, TRIPLEA_GAME_PROPERTY, TRIPLEA_NAME_PROPERTY,
					TRIPLEA_SERVER_PASSWORD_PROPERTY, TRIPLEA_STARTED, LOBBY_PORT, LOBBY_HOST, LOBBY_GAME_COMMENTS, LOBBY_GAME_HOSTED_BY };
		// if only 1 arg, it must be the game name
		// find it
		// optionally, it may not start with the property name
		if (args.length == 1)
		{
			String value;
			if (args[0].startsWith(TRIPLEA_GAME_PROPERTY))
			{
				value = getValue(args[0]);
			}
			else
			{
				value = args[0];
			}
			System.setProperty(TRIPLEA_GAME_PROPERTY, value);
			return;
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
	
	private static void usage()
	{
		System.out.println("Arguments\n" + "   triplea.game=<FILE_NAME>\n" + "   triplea.server=true\n" + "   triplea.client=true\n" + "   triplea.port=<PORT>\n" + "   triplea.host=<HOST>\n"
					+ "   triplea.name=<PLAYER_NAME>\n" + "\n" + "if there is only one argument, and it does not start with triplea.game, the argument will be \n"
					+ "taken as the name of the file to load.\n" + "\n" + "Example\n" + "   to start a game using the given file:\n" + "\n" + "   triplea /home/sgb/games/test.xml\n" + "\n"
					+ "   or\n" + "\n" + "   triplea triplea.game=/home/sgb/games/test.xml\n" + "\n" + "   to connect to a remote host:\n" + "\n"
					+ "   triplea triplea.client=true triplea.host=127.0.0.0 triplea.port=3300 triplea.name=Paul\n" + "\n" + "   to start a server with the given game\n" + "\n"
					+ "   triplea triplea.game=/home/sgb/games/test.xml triplea.server=true triplea.port=3300 triplea.name=Allan" + "\n"
					+ "   to start a server, you can optionally password protect the game using triplea.server.password=foo");
	}
	
	private static String getValue(final String arg)
	{
		final int index = arg.indexOf('=');
		if (index == -1)
			return "";
		return arg.substring(index + 1);
	}
	
	public static void setupLookAndFeel()
	{
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					try
					{
						UIManager.setLookAndFeel(getDefaultLookAndFeel());
					} catch (final Throwable t)
					{
						if (!GameRunner.isMac())
						{
							try
							{
								UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
							} catch (final Exception e)
							{
							}
						}
					}
				}
			});
		} catch (final Throwable t)
		{
			t.printStackTrace(System.out);
		}
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
	
	public static String getDefaultLookAndFeel()
	{
		final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
		// String defaultLookAndFeel = "org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel";
		String defaultLookAndFeel = "org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel";
		// macs are already beautiful
		if (GameRunner.isMac())
		{
			defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
		}
		final String userDefault = pref.get(LOOK_AND_FEEL_PREF, defaultLookAndFeel);
		final List<String> availableSkins = TripleaMenu.getLookAndFeelAvailableList();
		if (!availableSkins.contains(userDefault))
		{
			if (!availableSkins.contains(defaultLookAndFeel))
				return UIManager.getSystemLookAndFeelClassName();
			setDefaultLookAndFeel(defaultLookAndFeel);
			return defaultLookAndFeel;
		}
		return userDefault;
	}
	
	public static void setDefaultLookAndFeel(final String lookAndFeelClassName)
	{
		final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
		pref.put(LOOK_AND_FEEL_PREF, lookAndFeelClassName);
		try
		{
			pref.sync();
		} catch (final BackingStoreException e)
		{
			e.printStackTrace();
		}
	}
	
	public static boolean getDelayedParsing()
	{
		final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
		return pref.getBoolean(DELAYED_PARSING, true);
	}
	
	public static void setDelayedParsing(final boolean delayedParsing)
	{
		final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
		pref.putBoolean(DELAYED_PARSING, delayedParsing);
		try
		{
			pref.sync();
		} catch (final BackingStoreException e)
		{
			e.printStackTrace();
		}
	}
}
