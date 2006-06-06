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
import java.awt.event.*;
import java.awt.*;
import java.net.InetAddress;
import games.strategy.net.*;
import games.strategy.engine.lobby.ILobby;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.GameRunner2;
/**
 * AddServerConfig.java
 *
 * Created on May 25, 2006, 6:52 PM
 *
 * @author Harry
 */
public class AddServerConfig extends JFrame
{
    ILobby m_lobby;
    IMessenger m_messenger;
    MainFrame m_frame;
    JLabel m_ldesc;
    JTextField m_desc;
    JLabel m_lport;
    JTextField m_port;
    JButton m_add;
    LobbyClient m_lc;
    /** Creates a new instance of AddServerConfig */
    public AddServerConfig(ILobby lobby,IMessenger messenger,MainFrame frame,LobbyClient lc)
    {
        super("Add a server");
        m_lc = lc;
        m_lobby = lobby;
        m_messenger = messenger;
        m_frame = frame;
        m_ldesc = new JLabel("Server Description:");
        m_desc = new JTextField(messenger.getLocalNode().getName() + "'s game",0);
        m_lport = new JLabel("Port:");
        m_port = new JTextField("3300",0);
        m_add = new JButton("Add Server");
        m_add.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(m_desc.getText().length() != 0)
                {
                    String m_name = m_messenger.getLocalNode().getName();
                    InetAddress m_addr = m_messenger.getLocalNode().getAddress();
                    int m_iport;
                    try
                    {
                        m_iport = Integer.valueOf(m_port.getText()).intValue();
                    }
                    catch(Exception ex)
                    {
                        //notify about invalid port....
                        return;
                    }
                    
                    System.setProperty(GameRunner2.TRIPLEA_CLIENT_PROPERTY,"false");
                    System.setProperty(GameRunner2.TRIPLEA_SERVER_PROPERTY,"true");
                    System.setProperty(GameRunner2.TRIPLEA_NAME_PROPERTY,m_name);
                    System.setProperty(GameRunner2.TRIPLEA_PORT_PROPERTY,m_port.getText());
                    System.setProperty(LobbyClient.SERVER_HOSTING,"true");
                    System.setProperty(LobbyClient.SERVER_DESC,m_desc.getText());
                    System.setProperty(LobbyClient.SERVER_ADDR,m_addr.toString());
                    System.setProperty(LobbyClient.SERVER_PORT,m_port.getText());
                    m_frame.start();
                    INode n = new Node(m_desc.getText(),m_addr,m_iport);
                    m_lc.setSeverNode(n);
                    m_lobby.addServer(n);
                    setVisible(false);
                }
            }
        });
        setSize(300,125);
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1.0;
        c.weighty = 0.33333;
        JPanel p_desc = new JPanel();
        p_desc.setLayout(new BorderLayout());
        p_desc.add(m_ldesc,BorderLayout.LINE_START);
        p_desc.add(m_desc,BorderLayout.LINE_END);
        getContentPane().add(p_desc,c);
        JPanel p_port = new JPanel();
        p_port.setLayout(new BorderLayout());
        p_port.add(m_lport,BorderLayout.LINE_START);
        p_port.add(m_port,BorderLayout.LINE_END);
        c.gridy = 1;
        getContentPane().add(p_port,c);
        c.gridy = 2;
        getContentPane().add(m_add,c);
        validate();
    }
}
