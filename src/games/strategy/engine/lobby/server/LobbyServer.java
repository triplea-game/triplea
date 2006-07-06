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
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.util.Version;

import java.io.IOException;
import java.util.logging.*;

/**
 * LobbyServer.java
 * 
 * Created on May 23, 2006, 6:44 PM
 * 
 * @author Harry
 */
public class LobbyServer
{
    
    private final static Logger s_logger = Logger.getLogger(LobbyServer.class.getName());
    public static final String LOBBY_CHAT = "games.strategy.engine.lobby.client.ui.LOBBY_CHAT";
    public static final Version LOBBY_VERSION = new Version(1, 0, 0);
    

    private final IServerMessenger m_server;
    private final IRemoteMessenger m_remote;
    private final IChannelMessenger m_channel;
    private final UnifiedMessenger m_um;

    /** Creates a new instance of LobbyServer */
    public LobbyServer(String name, int port)
    {

        try
        {
            m_server = new ServerMessenger(name, port);
        } catch (IOException ex)
        {
            s_logger.log(Level.SEVERE, ex.toString());
            throw new IllegalStateException(ex.getMessage());
        }

        m_um = new UnifiedMessenger(m_server);
        m_remote = new RemoteMessenger(m_um);
        m_channel = new ChannelMessenger(m_um);

        m_server.setLoginValidator(new LobbyLoginValidator());

        // setup common objects
        new ChatController(LOBBY_CHAT, m_server, m_remote, m_channel);
        new UserManager().register(m_remote);
        
        m_channel.createChannel(ILobbyGameBroadcaster.class, ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL);
        LobbyGameController controller = new LobbyGameController((ILobbyGameBroadcaster) m_channel.getChannelBroadcastor(ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL), m_server);
        controller.register(m_remote);
        
        
        
        //now we are open for business
        m_server.setAcceptNewConnections(true);
    }

   

   
    public static void main(String args[])
    {
        if (args.length != 2)
        {
            System.out.println("Usage: LobbyServer [servername] [port]");
            return;
        }
        try
        {
            int p = Integer.parseInt(args[1]);
            new LobbyServer(args[0], p);

            System.out.println("Lobby started");
        } catch (Exception ex)
        {
            System.out.println(ex.toString());
        }
    }
}
