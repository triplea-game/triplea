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
 * ClientOptions.java
 *
 * Created on February 1, 2002, 1:50 PM
 */
package games.strategy.engine.framework.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.ui.IntTextField;

/**
 * UI for choosing server options.
 *
 * @author  Sean Bridges
 */
public class ClientOptions extends JFrame
{
	
	private JTextField m_nameField;
	private JTextField m_addressField;
	private IntTextField m_portField;
	private Object m_lock = new Object();
	
	/**
	 * Creates a new instance of ServerOptions
	 */
	public ClientOptions(String defaultName, int defaultPort, String defaultAddress)
	{
		super("Client options");
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		initComponents();
		layoutComponents();
		
		m_nameField.setText(defaultName);
		m_portField.setValue(defaultPort);
		m_addressField.setText(defaultAddress);
		
		pack();
	}
	
	public void waitForOptions()
	{
		try
		{
			synchronized(m_lock)
			{
				m_lock.wait();
			}
		} catch(InterruptedException ie)
		{}
	}

	public String getName()
	{
		return m_nameField.getText();
	}
	
	public String getAddress()
	{
		return m_addressField.getText();
	}
	
	public int getPort()
	{
		return m_portField.getValue();
	}
	
	private void initComponents()
	{
		m_nameField = new JTextField(10);
		m_addressField = new JTextField(10);
		m_portField = new IntTextField(0, Integer.MAX_VALUE);
		m_portField.setColumns(7);
	}
	
	private void layoutComponents()
	{
		Container content = getContentPane();
		content.setLayout(new BorderLayout());

		JPanel title = new JPanel();
		title.add(new JLabel("Select client options"));
		content.add(title, BorderLayout.NORTH);
		
		Insets labelSpacing = new Insets(3,7,0,0);
		Insets fieldSpacing = new Insets(3,5,0,7);
		
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.gridx = 0;
		labelConstraints.insets = labelSpacing;
		
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.anchor = GridBagConstraints.WEST;
		fieldConstraints.gridx = 1;
		fieldConstraints.insets = fieldSpacing;
		
		JPanel fields = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		
		fields.setLayout(layout);
		
		JLabel nameLabel = new JLabel("Name:");
		JLabel portLabel = new JLabel("Port:");
		JLabel addressLabel = new JLabel("Address:");
		
		layout.setConstraints(portLabel, labelConstraints);
		layout.setConstraints(nameLabel, labelConstraints);
		layout.setConstraints(addressLabel, labelConstraints);
		
		layout.setConstraints(m_portField, fieldConstraints);
		layout.setConstraints(m_nameField, fieldConstraints);
		layout.setConstraints(m_addressField, fieldConstraints);
		
		fields.add(nameLabel);
		fields.add(m_nameField);
		fields.add(portLabel);
		fields.add(m_portField);
		fields.add(addressLabel);
		fields.add(m_addressField);
		
		content.add(fields, BorderLayout.CENTER);
		
		JPanel buttons = new JPanel();
		buttons.add(new JButton(m_okAction));
		buttons.add(new JButton(m_cancelAction));
		
		content.add(buttons, BorderLayout.SOUTH);
	}
	
	private Action m_okAction = new AbstractAction("Connect")
	{
		public void actionPerformed(ActionEvent e)
		{
			synchronized(m_lock)
			{
				setVisible(false);
				m_lock.notifyAll();
			}
		}
	};
	
	private Action m_cancelAction = new AbstractAction("Cancel")
	{
		public void actionPerformed(ActionEvent e)
		{
			System.exit(0);
		}
	};
}