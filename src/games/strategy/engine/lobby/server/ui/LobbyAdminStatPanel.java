/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.lobby.server.ui;

import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.triplea.ui.MemoryLabel;

import java.awt.GridLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class LobbyAdminStatPanel extends JPanel
{
	private static final long serialVersionUID = 3737079270721494810L;
	private JLabel m_upSince;
	private JLabel m_maxPlayersLabel;
	private JLabel m_totalLoginsLabel;
	private JLabel m_currentLoginsLabel;
	private int m_maxPlayers;
	private int m_totalLogins;
	private int m_currentLogins;
	private final IMessenger m_messenger;
	
	public LobbyAdminStatPanel(final IMessenger messenger)
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
		setLayout(new GridLayout(5, 1));
		add(m_currentLoginsLabel);
		add(m_totalLoginsLabel);
		add(m_maxPlayersLabel);
		add(m_upSince);
		add(new MemoryLabel());
	}
	
	private void setupListeners()
	{
		((IServerMessenger) m_messenger).addConnectionChangeListener(new IConnectionChangeListener()
		{
			public void connectionRemoved(final INode to)
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
			
			public void connectionAdded(final INode to)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						m_currentLogins++;
						m_currentLoginsLabel.setText("Current Players: " + m_currentLogins);
						if (m_currentLogins > m_maxPlayers)
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
