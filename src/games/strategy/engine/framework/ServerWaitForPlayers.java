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
 * ServerWaitForPlayers.java
 *
 * Created on December 14, 2001, 12:58 PM
 */

package games.strategy.engine.framework;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import games.strategy.engine.framework.message.*;
import games.strategy.net.*;


/**
 *
 * @author  Sean Bridges
 */
public class ServerWaitForPlayers extends JFrame
{

	private IServerMessenger m_messenger;
	//collection of playerLabels
	private Collection m_players;
	private Object lock = new Object();
	private Collection m_resolvedPlayers = new ArrayList();
	
	/** Creates a new instance of ServerWaitForPlayers */
    public ServerWaitForPlayers(IServerMessenger messenger, Collection remotePlayers)
	{
		super("Wait for players to join");
		m_messenger= messenger;
		
		getContentPane().add(new JLabel("Address:" + messenger.getLocalNode().getAddress().getHostAddress()));
		getContentPane().add(new JLabel("Port:" + messenger.getLocalNode().getPort()));
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		
		m_players =  new ArrayList();
		Iterator iter = remotePlayers.iterator();
		while(iter.hasNext())
		{
			RemotePlayerLabel label = new RemotePlayerLabel( (String) iter.next());
			m_players.add(label);
			this.getContentPane().add(label);
		}
		this.pack();
    }
	
	/** 
	 * returns a mapping of PlayerName => INode
	 * Blocks the current thread, should not be called from
	 * the swing event thread.
	 */
	public Map waitForPlayers()
	{
		show();
		m_messenger.addMessageListener(m_listener);
		m_messenger.setAcceptNewConnections(true);
		synchronized(lock)
		{
			try
			{
				lock.wait();
				
				m_messenger.setAcceptNewConnections(false);
				m_messenger.removeMessageListener(m_listener);
				//TODO does this free the frame for gc?
				setVisible(false);
				
				Map playerMapping = new HashMap();
				
				Iterator iter = m_players.iterator();
				while(iter.hasNext())
				{
					RemotePlayerLabel label = (RemotePlayerLabel) iter.next();
					playerMapping.put(label.getName(), label.getNode());
				}
				
				return playerMapping;
			}
			catch(InterruptedException ie)
			{
				return waitForPlayers();
			}
		}
	}
		
	private IMessageListener m_listener = new IMessageListener()
	{
		
		public void messageReceived(Serializable msg, INode from)
		{
			if(! (msg instanceof PlayerSetupMessage))
				return;
				
			PlayerSetupMessage playerSetupMessage = (PlayerSetupMessage) msg;
			if(playerSetupMessage.isAvailable())
			{
				available(from);
			}
			else if(playerSetupMessage.isTake())
			{
				accept(from, playerSetupMessage.getNames());
			}	
		}
		
		private void available(INode to)
		{
			synchronized(ServerWaitForPlayers.this)
			{
				Iterator iter = m_players.iterator();
				Collection names = new ArrayList();
				while(iter.hasNext())
				{
					RemotePlayerLabel remote = (RemotePlayerLabel) iter.next();
					if(remote.getNode() == null)					
						names.add( remote.getName());
				}
				PlayerSetupMessage msg = new PlayerSetupMessage(names, PlayerSetupMessage.AVAILABLE);
				System.out.println("Server sending:" +  msg);
				m_messenger.send(msg , to);
			}
		}
		
		
		private void accept(INode from, Collection take)
		{
			synchronized(ServerWaitForPlayers.this)
			{
				ArrayList accepted = new ArrayList();
				
				Iterator iter = take.iterator();
				while(iter.hasNext())
				{
					String name = (String) iter.next();
					Iterator players = m_players.iterator();
					while(players.hasNext())
					{
						RemotePlayerLabel label = (RemotePlayerLabel) players.next();
						if(label.getName().equals(name) && label.getNode() == null)
						{
							label.setNode(from);
							m_resolvedPlayers.add(label.getName());
							accepted.add(label.getName());
						}
					}
				}
				PlayerSetupMessage response = new PlayerSetupMessage(accepted, PlayerSetupMessage.ACCEPTED);
				m_messenger.send(response, from);
				
				boolean done = m_resolvedPlayers.size() == m_players.size();
				if(done)
				{
					synchronized(lock)
					{
						lock.notifyAll();
					}
				}
			}
		}
	};
}


class RemotePlayerLabel extends JLabel
{
	private String m_name;
	private INode m_node;
	
	public RemotePlayerLabel(String name)
	{
		m_name = name;
		updateText();
	}
	
	public void setNode(INode node)
	{
		m_node = node;
		updateText();
	}
	
	public String getName()
	{
		return m_name;
	}
	
	private void updateText()
	{
		this.setText(m_name + " : " + getRemotePlayerText());
	}
	
	private String getRemotePlayerText()
	{
		if(m_node == null)
			return "None";
		
		return 	m_node.getName() + " at:" + m_node.getAddress().getHostAddress();
	}
	
	public INode getNode()
	{
		return m_node;
	}
}