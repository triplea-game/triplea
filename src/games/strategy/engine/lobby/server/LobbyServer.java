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
package games.strategy.engine.lobby.server;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.lobby.server.headless.HeadlessLobbyConsole;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.ui.LobbyAdminConsole;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.ClipPlayer;
import games.strategy.triplea.util.LoggingPrintStream;
import games.strategy.util.Version;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * LobbyServer.java
 * 
 * Created on May 23, 2006, 6:44 PM
 * 
 * @author Harry
 */
public class LobbyServer
{
	// System properties for the lobby
	// what port should the lobby use
	private static final String TRIPLEA_LOBBY_PORT_PROPERTY = "triplea.lobby.port";
	// should the lobby start a ui, set to true to enable
	private static final String TRIPLEA_LOBBY_UI_PROPERTY = "triplea.lobby.ui";
	// should the lobby take commands from stdin,
	// set to true to enable
	private static final String TRIPLEA_LOBBY_CONSOLE_PROPERTY = "triplea.lobby.console";
	
	public static final String ADMIN_USERNAME = "Admin";
	private final static Logger s_logger = Logger.getLogger(LobbyServer.class.getName());
	public static final String LOBBY_CHAT = "_LOBBY_CHAT";
	public static final Version LOBBY_VERSION = new Version(1, 0, 0);
	private final Messengers m_messengers;
	
	public static String[] getProperties()
	{
		return new String[] { TRIPLEA_LOBBY_PORT_PROPERTY, TRIPLEA_LOBBY_CONSOLE_PROPERTY, TRIPLEA_LOBBY_UI_PROPERTY };
	}
	
	/** Creates a new instance of LobbyServer */
	public LobbyServer(final int port)
	{
		IServerMessenger server;
		try
		{
			server = new ServerMessenger(ADMIN_USERNAME, port);
		} catch (final IOException ex)
		{
			s_logger.log(Level.SEVERE, ex.toString());
			throw new IllegalStateException(ex.getMessage());
		}
		m_messengers = new Messengers(server);
		server.setLoginValidator(new LobbyLoginValidator());
		// setup common objects
		new UserManager().register(m_messengers.getRemoteMessenger());
		final ModeratorController moderatorController = new ModeratorController(server, m_messengers);
		moderatorController.register(m_messengers.getRemoteMessenger());
		new ChatController(LOBBY_CHAT, m_messengers, moderatorController);
		// register the status controller
		final StatusManager statusManager = new StatusManager(m_messengers);
		// we dont need this manager now
		statusManager.shutDown();
		final LobbyGameController controller = new LobbyGameController(
					(ILobbyGameBroadcaster) m_messengers.getChannelMessenger().getChannelBroadcastor(ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL), server);
		controller.register(m_messengers.getRemoteMessenger());
		// now we are open for business
		server.setAcceptNewConnections(true);
	}
	
	private static void setUpLogging()
	{
		// setup logging to read our logging.properties
		try
		{
			LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("server-logging.properties"));
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
		Logger.getAnonymousLogger().info("Redirecting std out");
		System.setErr(new LoggingPrintStream("ERROR", Level.SEVERE));
		System.setOut(new LoggingPrintStream("OUT", Level.INFO));
	}
	
	public static void main(final String args[])
	{
		try
		{
			// send args to system properties
			handleCommandLineArgs(args);
			// turn off sound if no ui
			final boolean startUI = Boolean.parseBoolean(System.getProperty(TRIPLEA_LOBBY_UI_PROPERTY, "false"));
			if (!startUI)
				ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);
			// grab these before we override them with the loggers
			final InputStream in = System.in;
			final PrintStream out = System.out;
			setUpLogging();
			final int port = Integer.parseInt(System.getProperty(TRIPLEA_LOBBY_PORT_PROPERTY, "3303"));
			System.out.println("Trying to listen on port:" + port);
			final LobbyServer server = new LobbyServer(port);
			System.out.println("Starting database");
			// initialize the database
			Database.getConnection().close();
			s_logger.info("Lobby started");
			if (startUI)
			{
				startUI(server);
			}
			if (Boolean.parseBoolean(System.getProperty(TRIPLEA_LOBBY_CONSOLE_PROPERTY, "false")))
			{
				startConsole(server, in, out);
			}
		} catch (final Exception ex)
		{
			s_logger.log(Level.SEVERE, ex.toString(), ex);
		}
	}
	
	private static void startConsole(final LobbyServer server, final InputStream in, final PrintStream out)
	{
		System.out.println("starting console");
		new HeadlessLobbyConsole(server, in, out).start();
	}
	
	private static void startUI(final LobbyServer server)
	{
		System.out.println("starting ui");
		final LobbyAdminConsole console = new LobbyAdminConsole(server);
		console.setSize(800, 700);
		console.setLocationRelativeTo(null);
		console.setVisible(true);
	}
	
	public IServerMessenger getMessenger()
	{
		return (IServerMessenger) m_messengers.getMessenger();
	}
	
	public Messengers getMessengers()
	{
		return m_messengers;
	}
	
	/**
	 * Move command line arguments to System.properties
	 */
	private static void handleCommandLineArgs(final String[] args)
	{
		System.getProperties().setProperty(HeadlessGameServer.TRIPLEA_HEADLESS, "true");
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
	}
	
	private static String getValue(final String arg)
	{
		final int index = arg.indexOf('=');
		if (index == -1)
			return "";
		return arg.substring(index + 1);
	}
	
	private static void usage()
	{
		System.out.println("Arguments\n"
					+ "   " + TRIPLEA_LOBBY_PORT_PROPERTY + "=<port number (ex: 3303)>\n"
					+ "   " + TRIPLEA_LOBBY_UI_PROPERTY + "=<true/false>\n"
					+ "   " + TRIPLEA_LOBBY_CONSOLE_PROPERTY + "=<true/false>\n");
	}
}
