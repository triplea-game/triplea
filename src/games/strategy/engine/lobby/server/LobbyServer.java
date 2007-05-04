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
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.ui.LobbyAdminConsole;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.ServerMessenger;
import games.strategy.triplea.util.LoggingPrintStream;
import games.strategy.util.Version;

import java.io.IOException;
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
            setUpLogging();
            
            int port;
            if(args.length == 1)
                port =Integer.parseInt(args[0]);
            else
                port = 3302;
            
            LobbyServer server = new LobbyServer( port);

            //initialize the databse
            Database.getConnection().close();
            
            System.out.println("Lobby started");
            
            LobbyAdminConsole console = new LobbyAdminConsole(server);
            console.setSize(800,700);
            console.setLocationRelativeTo(null);
            console.setVisible(true);
        } catch (Exception ex)
        {
            System.out.println(ex.toString());
        }
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
