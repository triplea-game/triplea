/*
 * ClientPlayerSelector
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
public class ClientPlayerSelector extends JFrame
{
	
	private IMessenger m_messenger;
	private JLabel m_stateLabel = new JLabel("");
	private DefaultListModel m_listModel = new DefaultListModel();
	private JList m_list = new JList(m_listModel);
	private Object lock = new Object();
	private INode m_server;
	private Collection m_players;
	
	/** Creates a new instance of ServerWaitForPlayers */
    public ClientPlayerSelector(IMessenger messenger, INode server)
	{
		super("Choose players");
		m_messenger = messenger;
		m_server = server;
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getContentPane().add(m_stateLabel);
		getContentPane().add(m_list);
		getContentPane().add(new JButton(m_queryAction));
		getContentPane().add(new JButton(m_acceptAction));
		pack();
    }
	
	/** 
	 * returns a collection of playernames that this node will play
	 * Blocks the current thread, should not be called from
	 * the swing event thread.
	 */
	public Collection waitToJoin()
	{
		m_messenger.addMessageListener(m_listener);
		show();
		synchronized(lock)
		{
			try
			{
				lock.wait();
				m_messenger.removeMessageListener(m_listener);
				//TODO does this free the frame for gc?
				setVisible(false);
				
				return m_players;
			}
			catch(InterruptedException ie)
			{
				return waitToJoin();
			}
		}
	}
		
	private IMessageListener m_listener = new IMessageListener()
	{
		public void messageReceived(Serializable msg, INode from)
		{
			
			if(!(msg instanceof PlayerSetupMessage))
				return;
			
			PlayerSetupMessage playerSetupMessage = (PlayerSetupMessage) msg;
			
			if(playerSetupMessage.isAccepted())
			{
				m_players = playerSetupMessage.getNames();
				synchronized(lock)
				{
					lock.notifyAll();
				}
			}
			else if(playerSetupMessage.isAvailable())
			{
				m_listModel.removeAllElements();
				System.out.println(playerSetupMessage + " from:" + from);
				Iterator iter = playerSetupMessage.getNames().iterator();
				while(iter.hasNext())
				{
					m_listModel.addElement( (String) iter.next());
				}
				m_list.invalidate();
				pack();
			}
		}
	};
	
	private AbstractAction m_queryAction = new AbstractAction("Query")
	{
		public void actionPerformed(ActionEvent e)
		{
			m_messenger.send(new PlayerSetupMessage(null,PlayerSetupMessage.AVAILABLE), m_server);
		}
	};
	
	private AbstractAction m_acceptAction = new AbstractAction("Accept")
	{
		public void actionPerformed(ActionEvent e)
		{
			
			Object[] objects = m_list.getSelectedValues();
			if(objects.length == 0)
				return;
			
			ArrayList selected = new ArrayList();
			for(int i = 0; i < objects.length; i++)
			{
				selected.add(objects[i]);
			}
			
			PlayerSetupMessage msg = new PlayerSetupMessage(selected, PlayerSetupMessage.TAKE);
			m_messenger.send(msg, m_server);
		}
	};
}