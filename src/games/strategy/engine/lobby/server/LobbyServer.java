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
import games.strategy.engine.lobby.*;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;

import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.util.Version;

import java.io.IOException;
import java.util.*;
import java.util.logging.*;

/**
 * LobbyServer.java
 * 
 * Created on May 23, 2006, 6:44 PM
 * 
 * @author Harry
 */
public class LobbyServer implements ILobby
{
    private final static Logger s_logger = Logger.getLogger(LobbyServer.class.getName());

    public static final String LOBBY_CHAT = "games.strategy.engine.lobby.client.ui.LOBBY_CHAT";

    public static final Version LOBBY_VERSION = new Version(1, 0, 0);

    private ChatController m_cc;

    private ILobbyBrodcaster m_brodcast;

    private IServerMessenger m_server;

    private IRemoteMessenger m_remote;

    private IChannelMessenger m_channel;

    private UnifiedMessenger m_um;

    private final Object m_mutex = new Object();

    private List<INode> m_serverlist = new ArrayList<INode>();

    /** Creates a new instance of LobbyServer */
    public LobbyServer(String name, int port)
    {

        try
        {
            m_server = new ServerMessenger(name, port);
        } catch (IOException ex)
        {
            s_logger.log(Level.SEVERE, ex.toString());
            return;
        }

        m_um = new UnifiedMessenger(m_server);
        m_remote = new RemoteMessenger(m_um);
        m_channel = new ChannelMessenger(m_um);

        m_server.setLoginValidator(new LobbyLoginValidator());

        // setup common objects
        new ChatController(LOBBY_CHAT, m_server, m_remote, m_channel);
        new UserManager().register(m_remote);

        m_remote.registerRemote(ILobby.class, this, "lobby.lobby");
        m_channel.createChannel(ILobbyBrodcaster.class, "lobby.lobbybrodcaster");
        m_brodcast = (ILobbyBrodcaster) m_channel.getChannelBroadcastor("lobby.lobbybrodcaster");

        m_server.addConnectionChangeListener(new IConnectionChangeListener()
        {

            public void connectionRemoved(INode to)
            {
                forceRemoveServer(to);
            }

            public void connectionAdded(INode to)
            {
            }

        });
        
        // and we are open for business
        m_server.setAcceptNewConnections(true);

    }

    public void addServer(INode server)
    {
        synchronized (m_mutex)
        {
            s_logger.log(Level.INFO, "addserver: " + server.toString());
            m_serverlist.add(server);
            m_brodcast.serverAdded(server);
        }
    }

    public void forceRemoveServer(INode server)
    {
        synchronized (m_mutex)
        {
            for (INode t : m_serverlist)
            {
                if (t.getAddress().equals(server.getAddress()))
                {
                    m_serverlist.remove(t);
                    m_brodcast.serverRemoved(t);
                    return;
                }
            }
        }
    }

    public void removeServer(INode server)
    {
        synchronized (m_mutex)
        {
            s_logger.log(Level.INFO, "removeServer: " + server.toString());
            m_serverlist.remove(server);
            m_brodcast.serverRemoved(server);
        }
    }

    public ArrayList<INode> getServers()
    {
        synchronized (m_mutex)
        {
            s_logger.log(Level.INFO, "Server list requested.... servers:");
            for (INode n : m_serverlist)
            {
                s_logger.log(Level.INFO, n.toString());
            }
            s_logger.log(Level.INFO, "end of servers.");
            return new ArrayList<INode>(m_serverlist);
        }
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
