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

import javax.swing.*;
import java.util.*;
import java.net.InetAddress;
import java.awt.*;
import java.awt.event.*;
import games.strategy.net.*;
import games.strategy.engine.lobby.*;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.GameRunner2;
/**
 * ServersPanel.java
 *
 * Created on May 23, 2006, 7:40 PM
 *
 * @author Harry
 */
public class ServersPanel extends JPanel implements ILobbyBrodcaster
{
    JList m_serverlist;
    JButton m_add,m_remove,m_refresh,m_connect;
    ILobby m_lobby;
    IMessenger m_messenger;
    MainFrame m_frame;
    ArrayList<INode> m_snodes = new ArrayList();
    DefaultListModel m_servers;

    private final String NO_SERVERS = "no servers available!";
    
    private Runnable refreshServers = new Runnable()
    {
       public void run()
        {
            m_snodes = m_lobby.getServers();
            m_servers.clear();
            if(m_snodes.size() == 0)
            {
                m_servers.addElement(NO_SERVERS);
            }
            else
            {
                for(INode n : m_snodes)
                {
                    m_servers.addElement(n.getName());
                }
            }
            
        }
    };
    public ServersPanel(ILobby lobby,IMessenger messenger,MainFrame frame)
    {
        m_lobby = lobby;
        m_messenger = messenger;
        m_frame = frame;
        initComponents();
        setVisible(true);
        initActions();
        SwingUtilities.invokeLater(refreshServers);
    }
    
    void initComponents()
    {
        Container content = this;
        
        m_servers = new DefaultListModel();
        m_servers.addElement(NO_SERVERS);
        m_serverlist = new JList(m_servers);
        m_add = new JButton("add");
        m_remove = new JButton("remove");
        m_refresh = new JButton("refresh");
        m_connect = new JButton("connect");
        
        content.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 0.75;
        c.gridx = 0;
        c.gridy = 0;
        content.add(m_serverlist,c);
        c.weighty = 0.25;
        c.gridy = 1;
        JPanel pan = new JPanel();
        pan.setLayout(new GridLayout(2,2));
        pan.add(m_add);
        pan.add(m_remove);
        pan.add(m_refresh);
        pan.add(m_connect);
        content.add(pan,c);
    }
    
    void initActions()
    {
        m_add.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("addserver");
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        new AddServerConfig(m_lobby,m_messenger,m_frame).setVisible(true);
                    }
                });
            }
        });
        m_remove.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        if(System.getProperty(LobbyClient.SERVER_HOSTING).equals("true"))
                        {
                            String desc = System.getProperty(LobbyClient.SERVER_DESC);
                            InetAddress addr;
                            try
                            {
                                addr = InetAddress.getByName(System.getProperty(LobbyClient.SERVER_ADDR));
                            }
                            catch(Exception ex)
                            {
                                addr = m_messenger.getLocalNode().getAddress();
                            }
                            int port = Integer.valueOf(System.getProperty(LobbyClient.SERVER_PORT)).intValue();
                            INode m_node = new Node(desc,addr,port);
                            m_lobby.removeServer(m_node);
                            System.setProperty(LobbyClient.SERVER_HOSTING,"false");
                        }
                    }
                });
            }
        });
        m_refresh.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                System.out.println("refreshserver");
                SwingUtilities.invokeLater(refreshServers);
            }
        });
        m_connect.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        INode server_node = null;
                        if(m_snodes != null)
                        {
                            for(INode s : m_snodes)
                            {
                                if(s.getName() == (String)m_serverlist.getSelectedValue())
                                {
                                    server_node = s;
                                    break;
                                }
                            }
                        }
                        if(server_node != null)
                        {
                            if(m_frame == null)
                            {
                              // any extra settings?   
                            }
                            System.setProperty(GameRunner2.TRIPLEA_CLIENT_PROPERTY,"true");
                            System.setProperty(GameRunner2.TRIPLEA_SERVER_PROPERTY,"false");
                            System.setProperty(GameRunner2.TRIPLEA_NAME_PROPERTY,m_messenger.getLocalNode().getName());
                            System.setProperty(GameRunner2.TRIPLEA_HOST_PROPERTY,server_node.getAddress().toString());
                            System.setProperty(GameRunner2.TRIPLEA_PORT_PROPERTY,String.valueOf(server_node.getPort()));
                            m_frame.start();
                        }
                    }
                });
            }
        });
    }
    
    public void serverAdded(final INode server)
    {
        SwingUtilities.invokeLater(new Runnable(){
            public void run()
            {
                if(m_snodes.size() == 0)
                {
                    m_servers.clear();
                }
                m_snodes.add(server);
                m_servers.addElement(server.getName());
            }
        });
    }
    
    public void serverRemoved(final INode server)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_snodes.remove(server);
                if(m_snodes.size() == 0)
                {
                    m_servers.addElement(NO_SERVERS);
                }
                m_servers.removeElement(server.getName());
            }
        });
    }
}
