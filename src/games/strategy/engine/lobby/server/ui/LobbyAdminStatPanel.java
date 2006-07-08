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

import java.awt.GridLayout;
import java.util.Date;

import games.strategy.net.*;

import javax.swing.*;

public class LobbyAdminStatPanel extends JPanel
{

    private JLabel m_upSince;
    private JLabel m_maxPlayersLabel;
    private JLabel m_totalLoginsLabel;
    private JLabel m_currentLoginsLabel;
    private int m_maxPlayers;
    private int m_totalLogins;
    private int m_currentLogins;
    private final IMessenger m_messenger;
    
    public LobbyAdminStatPanel(IMessenger messenger)
    {
        m_messenger = messenger;
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {
        m_currentLoginsLabel = new JLabel("Current Players: -----");
        m_maxPlayersLabel = new JLabel("Max Concurrent Players : ----");
        m_totalLoginsLabel = new JLabel("Total Logins : ------");
        m_upSince = new JLabel("Up since " + new Date());
        
    }

    private void layoutComponents()
    {
        setLayout(new GridLayout(4,1));
        add(m_currentLoginsLabel);
        add(m_totalLoginsLabel);
        add(m_maxPlayersLabel);
        add(m_upSince);
    }

    private void setupListeners()
    {
        m_messenger.addConnectionChangeListener(new IConnectionChangeListener()
        {
        
            public void connectionRemoved(INode to)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                        m_currentLogins--;
                        m_currentLoginsLabel.setText("Current Players: " + m_currentLogins);
                    }
                
                });
        
            }
        
            public void connectionAdded(INode to)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                
                    public void run()
                    {
                        m_currentLogins++;
                        m_currentLoginsLabel.setText("Current Players: " + m_currentLogins);
                        
                        if(m_currentLogins > m_maxPlayers)
                        {
                            m_maxPlayers = m_currentLogins;
                            m_maxPlayersLabel.setText("Max Concurrent Players : " + m_maxPlayers);
                        }
                        m_totalLogins++;
                        m_totalLoginsLabel.setText("Total Logins : " + m_totalLogins);
                        
                    }
                
                });

        
            }
        
        });
        
    }

    private void setWidgetActivation()
    {

    }
}
