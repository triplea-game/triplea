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

package games.strategy.engine.lobby.server.ui;

import games.strategy.engine.framework.networkMaintenance.BootPlayerAction;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.userDB.Database;

import java.awt.BorderLayout;
import java.awt.event.*;

import javax.swing.*;

public class LobbyAdminConsole extends JFrame
{
    private final LobbyServer m_server;
    
    private JButton m_backupNow;
    private JButton m_exit;
    private JButton m_bootPlayer;
    private DBExplorerPanel m_executor;
    private AllUsersPanel m_allUsers;
    
    public LobbyAdminConsole(LobbyServer server)
    {
        super("Lobby Admin Console");
        m_server = server;
         
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {
        m_backupNow = new JButton("Backup Now");
        m_bootPlayer = new JButton("Boot Player");
        m_exit = new JButton("Exit");
        m_executor = new DBExplorerPanel();
        m_allUsers = new AllUsersPanel(m_server.getMessenger());

    }

    private void layoutComponents()
    {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(m_exit);
        toolBar.add(m_bootPlayer);
        toolBar.add(m_backupNow);
        add(toolBar, BorderLayout.NORTH);
        
        add(m_executor, BorderLayout.CENTER);
        add(m_allUsers, BorderLayout.EAST);
    }

    private void setupListeners()
    {
        m_bootPlayer.addActionListener(new BootPlayerAction(this, m_server.getMessenger() ));
        
        m_exit.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                int option = JOptionPane.showConfirmDialog(LobbyAdminConsole.this, "Are you Sure?", "Are you Sure", JOptionPane.YES_NO_OPTION);
                if(option != JOptionPane.YES_OPTION)
                    return;
                
                System.exit(0);
            }
        
        });
        
        m_backupNow.addActionListener(new ActionListener()
        {
        
            public void actionPerformed(ActionEvent e)
            {
                Database.backup();        
            }
        
        });
        
    }

    private void setWidgetActivation()
    {

    }
    
}
