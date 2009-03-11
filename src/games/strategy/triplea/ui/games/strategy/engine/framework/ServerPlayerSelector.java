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

/*
 * ServerPlayerSelecter.java
 *
 * Created on December 14, 2001, 9:06 AM
 */

package games.strategy.engine.framework;

import java.awt.FlowLayout;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;



/**
 *
 * @author  Sean Bridges
 */
public class ServerPlayerSelector extends JFrame
{
	private Collection<PlayerChoice> m_playerChoices;
	private Object m_lock = new Object();
	private Collection<String> m_remote;
	private JTextField m_nameField;
	
	/** Creates a new instance of PlayerSelecter */
    public ServerPlayerSelector(String[] players) 
	{
		super("Choose players");
		
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		namePanel.add(new JLabel("Name:"));
		
		m_nameField = new JTextField();
		m_nameField.setColumns(10);
		namePanel.add(m_nameField);
		getContentPane().add(namePanel);
		
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(new JLabel("Choose player location."));
		
		m_playerChoices = new ArrayList<PlayerChoice>();
		for(int i = 0; i < players.length; i++)
		{
			PlayerChoice current = new PlayerChoice(players[i]);
			m_playerChoices.add(current);
			getContentPane().add(current);
		}
	
		this.addWindowListener(m_doneOnClose);
		getContentPane().add(new JButton(m_done));
		this.pack();
		
    }
	
	/**
	 * Returns a collection of player names that are to be remote.
	 * This method blocks the current thread, and should
	 * not be called from the swing event thread.
	 */
	public Collection<String> getRemotePlayers()
	{
		try
		{
			synchronized(m_lock)
			{
				this.setVisible(true);
				m_lock.wait();
			}
			if(m_remote == null)
				return getRemotePlayers();
			else
				return m_remote;
		} catch(InterruptedException ie)
		{
			ie.printStackTrace();
			return getRemotePlayers();
		}
		
	}
	
	public String getName()
	{
		return m_nameField.getText();
	}
	
	public AbstractAction m_done = new AbstractAction("done")
	{
		public void actionPerformed(ActionEvent e)
		{
			if(!isVisible())
				return;
			
			m_remote = new ArrayList<String>();
			Iterator<PlayerChoice> iter = m_playerChoices.iterator();
			while(iter.hasNext())
			{
				PlayerChoice choice = iter.next();
				if(choice.isRemote())
				{
					m_remote.add(choice.getPlayerName());
				}
			}
			synchronized(m_lock)
			{
				m_lock.notifyAll();
			}
			setVisible(false);
			dispose();
		}
	};
	
	private WindowListener m_doneOnClose = new WindowAdapter()
	{
		public void windowClosing(WindowEvent e) 
		{
			m_done.actionPerformed(null);
		}
	};	
}

class PlayerChoice extends JPanel
{
	private static final String REMOTE = "Remote";
	private static final String LOCAL = "Local";
	private static final String[] s_choices = {LOCAL, REMOTE};
	
	private String m_name;
	private JComboBox m_choice;
	
	public PlayerChoice(String name)
	{
		setLayout(new FlowLayout(FlowLayout.LEFT));
		m_name = name;
		add(new JLabel(name));
		m_choice = new JComboBox(s_choices);
		m_choice.setSelectedIndex(0);
		m_choice.setEditable(false);
		add(m_choice);
	}
	
	public String getPlayerName()
	{
		return m_name;
	}
	
	public boolean isRemote()
	{
		return s_choices[m_choice.getSelectedIndex()].equals(REMOTE);
	}
}
