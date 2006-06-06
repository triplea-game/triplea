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
import java.awt.*;
import java.awt.event.*;
import games.strategy.engine.framework.startup.ui.MainFrame;
/**
 * LobbyClientConfigure.java
 *
 * Created on May 24, 2006, 11:02 PM
 *
 * @author Harry
 */
public class LobbyClientConfigure extends JFrame
{
    JButton m_connect;
    JTextField m_name;
    JTextField m_server;
    JTextField m_port;
    JLabel m_lname;
    JLabel m_lserver;
    JLabel m_lport;
    MainFrame m_frame;
    /** Creates a new instance of LobbyClientConfigure */
    public LobbyClientConfigure(MainFrame frame,String name) 
    {
        super("Connect to a lobby server");
        m_frame = frame;
        setSize(400,300);
        m_lname = new JLabel("User Name:");
        m_lserver = new JLabel("Server Address:");
        m_lport = new JLabel("port");
        m_connect = new JButton("connect");
        m_connect.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        int m_iport;
                        try
                        {
                            m_iport = Integer.valueOf(m_port.getText()).intValue();
                        }
                        catch(Exception ex)
                        {
                            //invalid port message
                            return ;
                        }
                        new LobbyClient(m_name.getText(),m_server.getText(),m_iport,m_frame);
                    }
                });
                setVisible(false);
            }
        });
        m_name = new JTextField(0);
        m_name.setText(name);
        m_server = new JTextField(0);
        m_port = new JTextField("3301");
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridy = 0;
        getContentPane().add(m_lname,c);
        c.gridx = 1;
        c.weightx = 1.0;
        getContentPane().add(m_name,c);
        c.gridy = 1;
        c.weightx = 0;
        c.gridx = 0;
        getContentPane().add(m_lserver,c);
        c.gridx = 1;
        c.weightx = 1.0;
        getContentPane().add(m_server,c);
        c.gridx = 0;
        c.weightx = 0;
        c.gridy = 2;
        getContentPane().add(m_lport,c);
        c.gridx = 1;
        c.weightx = 1.0;
        getContentPane().add(m_port,c);
        c.gridx = 0;
        c.weightx = 1.0;
        c.gridy = 3;
        getContentPane().add(m_connect,c);
        setVisible(true);
    }
    public static void main(final String[] args)
    {
        if(args.length == 0)
        {
            SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                       new LobbyClientConfigure(null,"");
                    }
                }
            );
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                       new LobbyClientConfigure(null,args[0]);
                    }
                }
            );
            
        }
    }
}
