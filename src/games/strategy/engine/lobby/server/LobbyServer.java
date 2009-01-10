/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.lobby.server;

import games.strategy.engine.chat.ChatController;
import games.strategy.engine.chat.StatusManager;
import games.strategy.engine.lobby.server.headless.HeadlessLobbyConsole;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.ui.LobbyAdminConsole;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
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
    
    //System properties for the lobby
    //what port should the lobby use
    private static final String PORT = "triplea.lobby.port";
    //should the lobby start a ui, set to true to enable
    private static final String UI = "triplea.lobby.ui";
    //should the lobby take commands from stdin, 
    //set to true to enable
    private static final String CONSOLE = "triplea.lobby.console";
    
    public static final String ADMIN_USERNAME = "Admin";
    private final static Logger s_logger = Logger.getLogger(LobbyServer.class.getName());
    public static final String LOBBY_CHAT = "_LOBBY_CHAT";
    public static final Version LOBBY_VERSION = new Version(1, 0, 0);
    

    private Messengers m_messengers;

    /** Creates a new instance of LobbyServer */
    public LobbyServer(int port)
    {

        IServerMessenger server;
        try
        {
            server = new ServerMessenger(ADMIN_USERNAME, port);
        } catch (IOException ex)
        {
            s_logger.log(Level.SEVERE, ex.toString());
            throw new IllegalStateException(ex.getMessage());
        }

        m_messengers = new Messengers(server);
        
        

        server.setLoginValidator(new LobbyLoginValidator());

        
        // setup common objects
        new ChatController(LOBBY_CHAT, m_messengers);
        //register the status controller
        StatusManager statusManager = new StatusManager(m_messengers);
        //we dont need this manager now
        statusManager.shutDown();
        new UserManager().register(m_messengers.getRemoteMessenger());
        new ModeratorController(server).register(m_messengers.getRemoteMessenger());
        
        
        LobbyGameController controller = new LobbyGameController((ILobbyGameBroadcaster) m_messengers.getChannelMessenger().getChannelBroadcastor(ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL), server);
        controller.register(m_messengers.getRemoteMessenger());
        
        //now we are open for business
        server.setAcceptNewConnections(true);
    }

    private static void setUpLogging() 
    {

        
        //  setup logging to read our logging.properties
        try
        {
            LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("server-logging.properties"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        
        Logger.getAnonymousLogger().info("Redirecting std out");
        System.setErr(new LoggingPrintStream("ERROR", Level.SEVERE));
        System.setOut(new LoggingPrintStream("OUT", Level.INFO));
        
    }
   
    public static void main(String args[])
    {
        
        
        try
        {
            //grab these before we override them with the loggers
            InputStream in = System.in;
            PrintStream out = System.out;
            
            setUpLogging();
            
            int port = Integer.parseInt(System.getProperty(PORT, "3303"));
            
            System.out.println("Trying to listen on port:" + port);
            LobbyServer server = new LobbyServer( port);

            
            System.out.println("Starting database");
            //initialize the databse
            Database.getConnection().close();
            
            s_logger.info("Lobby started");
            
            if(Boolean.parseBoolean(System.getProperty(UI, "false"))) {
                startUI(server);    
            }
            if(Boolean.parseBoolean(System.getProperty(CONSOLE, "false"))) {
                startConsole(server, in, out);    
            }  
            
        } catch (Exception ex)
        {
            s_logger.log(Level.SEVERE,  ex.toString(), ex);
        }
    }

    private static void startConsole(LobbyServer server, InputStream in, PrintStream out)
    {
        System.out.println("starting console");
        new HeadlessLobbyConsole(server, in, out).start();
        
    }

    private static void startUI(LobbyServer server)
    {
        System.out.println("starting ui");
        LobbyAdminConsole console = new LobbyAdminConsole(server);
        console.setSize(800,700);
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
}
