/*
 * TechPanel.java
 *
 * Created on December 5, 2001, 7:04 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.ui.*;
import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.message.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TechPanel extends ActionPanel
{
	private JLabel m_actionLabel = new JLabel();
	private IntegerMessage m_intMessage;
	
	/** Creates new BattlePanel */
    public TechPanel(GameData data, MapPanel map) 
	{
		super(data, map);
    }
		
	public void display(PlayerID id)
	{
		super.display(id);
		removeAll();
		m_actionLabel.setText(id.getName() + " Tech Roll");
		add(m_actionLabel);	
		add(new JButton(GetTechRollsAction));
		add(new JButton(DontBother));
		
	}
	
	public String toString()
	{
		return "TechPanel";
	}
	
	public IntegerMessage waitForTech()
	{
		try
		{
			synchronized(getLock())
			{
				getLock().wait();
			}
		} catch(InterruptedException ie)
		{
			waitForTech();
		}
		
		if(m_intMessage == null)
			return null;
		
		if(m_intMessage.getMessage() == 0)
			return null;

		return m_intMessage;
	}
	
	private Action GetTechRollsAction = new AbstractAction("Do Tech Roll")
	{
		public void actionPerformed(ActionEvent event)
		{
			int ipcs = getCurrentPlayer().getResources().getQuantity(Constants.IPCS);
			String message = "Select number of tech rolls";
			TechRollPanel techRollPanel = new TechRollPanel(ipcs);
			int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), techRollPanel, message, JOptionPane.OK_CANCEL_OPTION);
			if(choice != JOptionPane.OK_OPTION)
				return;
			
			int quantity = techRollPanel.getValue();
			m_intMessage =  new IntegerMessage(quantity);
			synchronized(getLock())
			{
				getLock().notifyAll();
			}
			
		}
	};
	
	private Action DontBother = new AbstractAction("Dont Bother")
	{
		public void actionPerformed(ActionEvent event)
		{
			synchronized(getLock())
			{
				m_intMessage = null;
				getLock().notifyAll();
			}
		}
	};

	
}

class TechRollPanel extends JPanel
{
	int m_ipcs;
	JLabel m_left = new JLabel();
	ScrollableTextField m_text;
	
	TechRollPanel(int ipcs)
	{
		setLayout(new BorderLayout());
		m_ipcs = ipcs;
		m_text = new ScrollableTextField(0, ipcs / Constants.TECH_ROLL_COST);
		m_text.addChangeListener(m_listener);
		setLabel(ipcs);
		add(m_text, BorderLayout.CENTER);
		add(m_left, BorderLayout.SOUTH);
	}
	
	private void setLabel(int ipcs)
	{
		m_left.setText("Left to spend:" + ipcs);
	}
	
	private ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
	{
		public void changedValue(ScrollableTextField stf)
		{
			setLabel(m_ipcs - (Constants.TECH_ROLL_COST * m_text.getValue()));
		}
	};
	
	public int getValue()
	{
		return m_text.getValue();
	}
}