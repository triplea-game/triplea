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
import games.strategy.engine.lobby.*;
import games.strategy.engine.message.*;
import games.strategy.engine.chat.*;
import games.strategy.net.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
/**
 * LobbyServer.java
 *
 * Created on May 23, 2006, 6:44 PM
 *
 * @author Harry
 */
public class LobbyServer implements ILobby
{
    LobbyChatController m_cc;
    ILobbyBrodcaster m_brodcast;
    private IServerMessenger m_server;
    private IRemoteMessenger m_remote;
    private IChannelMessenger m_channel;
    private UnifiedMessenger m_um;
    private final Object m_mutex = new Object();
    ArrayList<INode> m_serverlist = new ArrayList();
    /** Creates a new instance of LobbyServer */
    public LobbyServer(String name,int port)
    {
        try
       {
           m_server = new ServerMessenger(name,port);
       }
       catch(IOException ex)
       {
           //log
           System.out.println(ex.toString());
           return;
       }
       m_server.setAcceptNewConnections(true);
       m_um = new UnifiedMessenger(m_server);
       m_remote = new RemoteMessenger(m_um);
       m_channel = new ChannelMessenger(m_um);
       m_cc = new LobbyChatController("lobby.chat",m_server,m_remote,m_channel,this);
       m_remote.registerRemote(ILobby.class,this,"lobby.lobby");
       m_channel.createChannel(ILobbyBrodcaster.class,"lobby.lobbybrodcaster");
       m_brodcast = (ILobbyBrodcaster)m_channel.getChannelBroadcastor("lobby.lobbybrodcaster");
    }
    public void addServer(INode server)
    {
        synchronized(m_mutex)
        {
            //log
            System.out.println("addServer: " + server.toString());
            m_serverlist.add(server);
            m_brodcast.serverAdded(server);
        }
    }
    public void forceRemoveServer(INode server)
    {
        synchronized(m_mutex)
        {
            for(INode t : m_serverlist)
            {
                if(t.getAddress().equals(server.getAddress()))
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
        synchronized(m_mutex)
        {
            //log
            System.out.println("removeServer: " + server.toString());
            m_serverlist.remove(server);
            m_brodcast.serverRemoved(server);
        }
    }
    public ArrayList<INode> getServers()
    {
        synchronized(m_mutex)
        {
            //log
            System.out.println("Server list requested.... servers:");
            for(INode n : m_serverlist)
            {
                System.out.println(n.toString());
            }
            System.out.println("end of servers.");
            return new ArrayList(m_serverlist);
        }
    }
    public static void main(String args[])
    {
        if(args.length != 2)
        {
            System.out.println("Usage: LobbyServer [servername] [port]");
            return;
        }
        try
        {
            int p = Integer.parseInt(args[1]);
            LobbyServer ls = new LobbyServer(args[0],p);
        }
        catch(Exception ex)
        {
            System.out.println(ex.toString());
        }
    }
}
