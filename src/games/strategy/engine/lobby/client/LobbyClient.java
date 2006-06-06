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

package games.strategy.engine.lobby.client;

import games.strategy.engine.lobby.*;
import games.strategy.engine.message.*;
import games.strategy.net.*;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.MainFrame;
import javax.swing.*;
import java.awt.*;
import java.io.*;
/**
 * LobbyClient.java
 *
 * Created on May 23, 2006, 6:44 PM
 *
 * @author Harry
 */
public class LobbyClient extends JFrame
{
    ChatPanel m_cp;
    ServersPanel m_sp;
    
    IRemoteMessenger m_remote;
    IChannelMessenger m_channel;
    IMessenger m_client;
    UnifiedMessenger m_um;
    
    ILobby m_lobby;
    MainFrame m_frame;
    
    public static final String SERVER_HOSTING = "triplea.lobbyclient.server_hosting";
    public static final String SERVER_DESC = "triplea.lobbyclient.server_description";
    public static final String SERVER_ADDR = "triplea.lobbyclient.server_address";
    public static final String SERVER_PORT = "triplea.lobbyclient.server_port";
    
    /** Creates a new instance of LobbyClient */
    public LobbyClient(String name,String server,int port,MainFrame frame)
    {
        m_frame = frame;
        if(m_frame == null)
        {
            m_frame = new MainFrame();
        }
        try
        {
            initConnections(name,server,port);
            setVisible(true);
        }
        catch(Exception ex)
        {
            //log ex
            System.out.println(ex.toString());
            setVisible(false);
        }
        initComponents(name,server,port);
    }
    
    void initConnections(String name,String server,int port) throws Exception
    {
        m_client = new  ClientMessenger(server,port,name);
        m_um = new UnifiedMessenger(m_client);
        m_remote = new RemoteMessenger(m_um);
        m_channel = new ChannelMessenger(m_um);
        m_cp = new ChatPanel(m_client,m_channel,m_remote,"lobby.chat");
        m_remote.waitForRemote("lobby.lobby",200);
        m_lobby = (ILobby)m_remote.getRemote("lobby.lobby");
        m_sp = new ServersPanel(m_lobby,m_client,m_frame);
        m_channel.registerChannelSubscriber(m_sp,"lobby.lobbybrodcaster");
        setTitle("Welcome to " + m_client.getServerNode().getName() + ", " + m_client.getLocalNode().getName());
    }
    void initComponents(String name,String server,int port)
    {
        setSize(600,400);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.75;
        c.weighty = 1.00;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        getContentPane().add(m_cp,c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.weightx = 0.25;
        getContentPane().add(m_sp,c);
        validate();
    }
    
    public static void main(final String args[])
    {
        if(args.length != 3 && args.length != 0)
        {
            System.out.println("Usage: LobbyClient [username] [server address] [port]");
            System.out.println("Usage: LobbyClient");
            return;
        }
        if(args.length == 0)
        {
            LobbyClientConfigure.main(args);
            return;
        }
        try
        {
            SwingUtilities.invokeLater(
                new Runnable(){
                    public void run()
                    {
                        new LobbyClient(args[0],args[1],Integer.parseInt(args[2]),null);
                    }
                }
            );
        }
        catch(Exception ex)
        {
            System.out.println(ex.toString());
        }
    }
}
