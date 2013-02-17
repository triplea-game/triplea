package games.strategy.engine.framework;

import games.strategy.debug.Console;
import games.strategy.engine.EngineVersion;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.background.WaitWindow;
import games.strategy.triplea.ui.ErrorHandler;
import games.strategy.triplea.ui.TripleaMenu;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Version;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;
import java.util.logging.LogManager;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.httpclient.HostConfiguration;

public class GameRunner2
{
	// not arguments:
	public static final String LOOK_AND_FEEL_PREF = "LookAndFeel";
	public static final String DELAYED_PARSING = "DelayedParsing";
	public static final String PROXY_CHOICE = "proxy.choice";
	public static final String HTTP_PROXYHOST = "http.proxyHost";
	public static final String HTTP_PROXYPORT = "http.proxyPort";
	// do not include this in the getProperties list. they are only for loading an old savegame.
	public static final String OLD_EXTENSION = ".old";
	// argument options below:
	public static final String TRIPLEA_GAME_PROPERTY = "triplea.game";
	public static final String TRIPLEA_SERVER_PROPERTY = "triplea.server";
	public static final String TRIPLEA_CLIENT_PROPERTY = "triplea.client";
	public static final String TRIPLEA_HOST_PROPERTY = "triplea.host";
	public static final String TRIPLEA_PORT_PROPERTY = "triplea.port";
	public static final String TRIPLEA_NAME_PROPERTY = "triplea.name";
	public static final String TRIPLEA_SERVER_PASSWORD_PROPERTY = "triplea.server.password";
	public static final String TRIPLEA_STARTED = "triplea.started";
	// these properties are for games that should connect to the Lobby Server
	public static final String LOBBY_PORT = "triplea.lobby.port";
	public static final String LOBBY_HOST = "triplea.lobby.host";
	public static final String LOBBY_GAME_COMMENTS = "triplea.lobby.game.comments";
	public static final String LOBBY_GAME_HOSTED_BY = "triplea.lobby.game.hostedBy";
	// what is the default version of triplea (the one in the "bin" folder)
	public static final String TRIPLEA_ENGINE_VERSION_BIN = "triplea.engine.version.bin";
	// proxy stuff
	public static final String PROXY_HOST = "proxy.host";
	public static final String PROXY_PORT = "proxy.port";
	// other stuff
	public static final String TRIPLEA_DO_NOT_CHECK_FOR_UPDATES = "triplea.doNotCheckForUpdates";
	
	// first time we've run this version of triplea?
	private static final String TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY = "triplea.firstTimeThisVersion" + EngineVersion.VERSION.toString();
	
	private static WaitWindow waitWindow;
	
	
	public static enum ProxyChoice
	{
		NONE, USE_SYSTEM_SETTINGS, USE_USER_PREFERENCES
	}
	
	public static String[] getProperties()
	{
		return new String[] { TRIPLEA_GAME_PROPERTY, TRIPLEA_SERVER_PROPERTY, TRIPLEA_CLIENT_PROPERTY, TRIPLEA_HOST_PROPERTY, TRIPLEA_PORT_PROPERTY, TRIPLEA_NAME_PROPERTY,
					TRIPLEA_SERVER_PASSWORD_PROPERTY, TRIPLEA_STARTED, LOBBY_PORT, LOBBY_HOST, LOBBY_GAME_COMMENTS, LOBBY_GAME_HOSTED_BY, TRIPLEA_ENGINE_VERSION_BIN,
					PROXY_HOST, PROXY_PORT, TRIPLEA_DO_NOT_CHECK_FOR_UPDATES };
	}
	
	private static void usage()
	{
		System.out.println("Arguments\n"
					+ "   " + TRIPLEA_GAME_PROPERTY + "=<FILE_NAME>\n"
					+ "   " + TRIPLEA_SERVER_PROPERTY + "=true\n"
					+ "   " + TRIPLEA_CLIENT_PROPERTY + "=true\n"
					+ "   " + TRIPLEA_HOST_PROPERTY + "=<HOST_IP>\n"
					+ "   " + TRIPLEA_PORT_PROPERTY + "=<PORT>\n"
					+ "   " + TRIPLEA_NAME_PROPERTY + "=<PLAYER_NAME>\n"
					+ "   " + LOBBY_PORT + "=<LOBBY_PORT>\n"
					+ "   " + LOBBY_HOST + "=<LOBBY_HOST>\n"
					+ "   " + LOBBY_GAME_COMMENTS + "=<LOBBY_GAME_COMMENTS>\n"
					+ "   " + LOBBY_GAME_HOSTED_BY + "=<LOBBY_GAME_HOSTED_BY>\n"
					+ "   " + PROXY_HOST + "=<Proxy_Host>\n"
					+ "   " + PROXY_PORT + "=<Proxy_Port>\n"
					+ "\nExample\n" + "   to start a game using the given file:\n\n"
					+ "   triplea triplea.game=/home/sgb/games/test.xml\n" + "\n"
					+ "   to connect to a remote host:\n" + "\n"
					+ "   triplea triplea.client=true triplea.host=127.0.0.0 triplea.port=3300 triplea.name=Paul\n" + "\n"
					+ "   to start a server with the given game\n" + "\n"
					+ "   triplea triplea.game=/home/sgb/games/test.xml triplea.server=true triplea.port=3300 triplea.name=Allan" + "\n"
					+ "   to start a server, you can optionally password protect the game using triplea.server.password=foo");
	}
	
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
		// do after we handle command line arts
		setupProxies();
		showMainFrame();
		// lastly, check and see if there are new versions of TripleA out
		checkForLatestEngineVersionOut();
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
		final String[] properties = getProperties();
		
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
		final String version = System.getProperty(TRIPLEA_ENGINE_VERSION_BIN);
		if (version != null && version.length() > 0)
		{
			final Version testVersion;
			try
			{
				testVersion = new Version(version);
				// if successful we don't do anything
				System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + version);
				if (!EngineVersion.VERSION.equals(testVersion, false))
					System.out.println("Current Engine version in use: " + EngineVersion.VERSION.toString());
			} catch (final Exception e)
			{
				System.getProperties().setProperty(TRIPLEA_ENGINE_VERSION_BIN, EngineVersion.VERSION.toString());
				System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + EngineVersion.VERSION.toString());
				return;
			}
		}
		else
		{
			System.getProperties().setProperty(TRIPLEA_ENGINE_VERSION_BIN, EngineVersion.VERSION.toString());
			System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + EngineVersion.VERSION.toString());
		}
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
	
	private static void setupProxies()
	{
		String proxyHostArgument = System.getProperty(PROXY_HOST); // System properties, not user pref
		String proxyPortArgument = System.getProperty(PROXY_PORT);
		if (proxyHostArgument == null)
			proxyHostArgument = System.getProperty(HTTP_PROXYHOST); // in case it was set by -D we also check this
		if (proxyPortArgument == null)
			proxyPortArgument = System.getProperty(HTTP_PROXYPORT);
		// arguments should override and set user preferences
		// host
		String proxyHost = null;
		if (proxyHostArgument != null && proxyHostArgument.trim().length() > 0)
			proxyHost = proxyHostArgument;
		// port
		String proxyPort = null;
		if (proxyPortArgument != null && proxyPortArgument.trim().length() > 0)
		{
			try
			{
				Integer.parseInt(proxyPortArgument);
				proxyPort = proxyPortArgument;
			} catch (final NumberFormatException nfe)
			{
				nfe.printStackTrace();
			}
		}
		if (proxyHost != null || proxyPort != null)
			setProxy(proxyHost, proxyPort, ProxyChoice.USE_USER_PREFERENCES);
		final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
		final ProxyChoice choice = ProxyChoice.valueOf(pref.get(PROXY_CHOICE, ProxyChoice.NONE.toString()));
		if (choice == ProxyChoice.USE_SYSTEM_SETTINGS)
			setToUseSystemProxies();
		else if (choice == ProxyChoice.USE_USER_PREFERENCES)
		{
			final String host = pref.get(GameRunner2.PROXY_HOST, "");
			final String port = pref.get(GameRunner2.PROXY_PORT, "");
			if (host.trim().length() > 0)
				System.setProperty(HTTP_PROXYHOST, host);
			if (port.trim().length() > 0)
				System.setProperty(HTTP_PROXYPORT, port);
		}
	}
	
	public static void setProxy(final String proxyHost, final String proxyPort, final ProxyChoice proxyChoice)
	{
		final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
		final ProxyChoice choice;
		if (proxyChoice != null)
		{
			choice = proxyChoice;
			pref.put(PROXY_CHOICE, proxyChoice.toString());
		}
		else
		{
			choice = ProxyChoice.valueOf(pref.get(PROXY_CHOICE, ProxyChoice.NONE.toString()));
		}
		if (proxyHost != null && proxyHost.trim().length() > 0)
		{
			pref.put(PROXY_HOST, proxyHost); // user pref, not system properties
			if (choice == ProxyChoice.USE_USER_PREFERENCES)
				System.setProperty(HTTP_PROXYHOST, proxyHost);
		}
		if (proxyPort != null && proxyPort.trim().length() > 0)
		{
			try
			{
				Integer.parseInt(proxyPort);
				pref.put(PROXY_PORT, proxyPort); // user pref, not system properties
				if (choice == ProxyChoice.USE_USER_PREFERENCES)
					System.setProperty(HTTP_PROXYPORT, proxyPort);
			} catch (final NumberFormatException nfe)
			{
				nfe.printStackTrace();
			}
		}
		if (choice == ProxyChoice.NONE)
		{
			System.clearProperty(HTTP_PROXYHOST);
			System.clearProperty(HTTP_PROXYPORT);
		}
		else if (choice == ProxyChoice.USE_SYSTEM_SETTINGS)
		{
			setToUseSystemProxies();
		}
		if (proxyHost != null || proxyPort != null || proxyChoice != null)
		{
			try
			{
				pref.flush();
				pref.sync();
			} catch (final BackingStoreException e)
			{
				e.printStackTrace();
			}
		}
		/*System.out.println(System.getProperty(HTTP_PROXYHOST));
		System.out.println(System.getProperty(HTTP_PROXYPORT));*/
	}
	
	private static void setToUseSystemProxies()
	{
		final String JAVA_NET_USESYSTEMPROXIES = "java.net.useSystemProxies";
		System.setProperty(JAVA_NET_USESYSTEMPROXIES, "true");
		List<Proxy> proxyList = null;
		try
		{
			final ProxySelector def = ProxySelector.getDefault();
			if (def != null)
			{
				proxyList = def.select(new URI("http://sourceforge.net/"));
				ProxySelector.setDefault(null);
				if (proxyList != null && !proxyList.isEmpty())
				{
					final Proxy proxy = proxyList.get(0);
					final InetSocketAddress address = (InetSocketAddress) proxy.address();
					if (address != null)
					{
						final String host = address.getHostName();
						final int port = address.getPort();
						System.setProperty(HTTP_PROXYHOST, host);
						System.setProperty(HTTP_PROXYPORT, Integer.toString(port));
						System.setProperty(PROXY_HOST, host);
						System.setProperty(PROXY_PORT, Integer.toString(port));
					}
					else
					{
						System.clearProperty(HTTP_PROXYHOST);
						System.clearProperty(HTTP_PROXYPORT);
						System.clearProperty(PROXY_HOST);
						System.clearProperty(PROXY_PORT);
					}
				}
			}
			else
			{
				final String host = System.getProperty(PROXY_HOST);
				final String port = System.getProperty(PROXY_PORT);
				if (host == null)
					System.clearProperty(HTTP_PROXYHOST);
				else
					System.setProperty(HTTP_PROXYHOST, host);
				if (port == null)
					System.clearProperty(HTTP_PROXYPORT);
				else
				{
					try
					{
						Integer.parseInt(port);
						System.setProperty(HTTP_PROXYPORT, port);
					} catch (final NumberFormatException nfe)
					{
						// nothing
					}
				}
			}
		} catch (final Exception e)
		{
			e.printStackTrace();
		} finally
		{
			System.setProperty(JAVA_NET_USESYSTEMPROXIES, "false");
		}
	}
	
	public static void addProxy(final HostConfiguration config)
	{
		final String host = System.getProperty(HTTP_PROXYHOST);
		final String port = System.getProperty(HTTP_PROXYPORT, "-1");
		if (host != null && host.trim().length() > 0)
			config.setProxy(host, Integer.valueOf(port));
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
	
	public static void checkForLatestEngineVersionOut()
	{
		final Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					// do not check if we are the old extra jar. (a jar kept for backwards compatibility only)
					if (GameRunner.areWeOldExtraJar())
						return;
					// if we are joining a game online, or hosting, or loading straight into a savegame, do not check
					final String fileName = System.getProperty(GameRunner2.TRIPLEA_GAME_PROPERTY, "");
					if (fileName.trim().length() > 0)
						return;
					if (System.getProperty(GameRunner2.TRIPLEA_SERVER_PROPERTY, "false").equalsIgnoreCase("true"))
						return;
					if (System.getProperty(GameRunner2.TRIPLEA_CLIENT_PROPERTY, "false").equalsIgnoreCase("true"))
						return;
					if (System.getProperty(GameRunner2.TRIPLEA_DO_NOT_CHECK_FOR_UPDATES, "false").equalsIgnoreCase("true"))
						return;
					
					final EngineVersionProperties latestEngineOut = EngineVersionProperties.contactServerForEngineVersionProperties();
					if (latestEngineOut == null)
						return;
					try
					{
						Thread.sleep(2500);
					} catch (final InterruptedException e)
					{
					}
					if (EngineVersion.VERSION.isLessThan(latestEngineOut.getLatestVersionOut(), false))
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getOutOfDateComponent(), "Please Update TripleA", JOptionPane.INFORMATION_MESSAGE, false,
											new CountDownLatchHandler(true));
							}
						});
					}
					else
					{
						// if this is the first time we are running THIS version of TripleA, then show what is new.
						final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
						if ((pref.getBoolean(TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY, true)) && latestEngineOut.getReleaseNotes().containsKey(EngineVersion.VERSION))
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getCurrentFeaturesComponent(), "What is New?", JOptionPane.INFORMATION_MESSAGE, false,
												new CountDownLatchHandler(true));
								}
							});
							pref.putBoolean(TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY, false);
							try
							{
								pref.flush();
							} catch (final BackingStoreException ex)
							{
							}
						}
					}
				} catch (final Exception e)
				{
					System.out.println("Error while checking for updates: " + e.getMessage());
				}
			}
		}, "Checking Latest TripleA Engine Version");
		t.start();
	}
}
