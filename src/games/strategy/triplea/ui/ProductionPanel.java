/*
 * ProductionPanel.java
 *
 * Created on November 7, 2001, 10:19 AM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.triplea.image.*;
import games.strategy.engine.data.*;
import games.strategy.ui.*;
import games.strategy.util.*;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Constants;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * 
 */
public class ProductionPanel extends JPanel
{
	private static JFrame s_owner;
	private static JDialog s_dialog;
	private static ProductionPanel s_panel;
		
	private Collection m_rules = new ArrayList();
	private int m_limit;
	private int m_current;
	private JLabel m_left = new JLabel();
	private PlayerID m_id;
	
	/**
	 * Shows the production panel, and returns a map of 
	 * selected rules.
	 */
	public static IntegerMap show(PlayerID id, JFrame parent, GameData data)
	{
		if(!(parent == s_owner))
			s_dialog = null;
		
		if(s_dialog == null)
			initDialog(parent);
		
		s_panel.initRules(id.getProductionFrontier().getRules(), data);
		s_panel.initLayout(id);
		s_panel.calculateLimits();
		
		s_dialog.pack();
		s_dialog.show();
		
		return s_panel.getProduction();
		
	}
	
	
	
	private static void initDialog(JFrame root)
	{
		s_panel = new ProductionPanel();
		s_dialog = new JDialog(root, "Produce", true);
		s_dialog.getContentPane().add(s_panel);
	}
	
	/** Creates new ProductionPanel */
    private ProductionPanel() 
	{
		
    }
	
	private void initRules(Collection rules, GameData data)
	{
		Iterator iter = rules.iterator();
		while(iter.hasNext())
		{
			m_rules.add(new Rule( (ProductionRule) iter.next(), data));
		}
	
	}
	
	private void initLayout(PlayerID id)
	{
		m_id= id;
		this.removeAll();
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		int ipcs = m_id.getResources().getQuantity(Constants.IPCS);
		add(new JLabel("You have:" + ipcs + " Ipc's to spend"));
		Iterator iter = m_rules.iterator();
		while(iter.hasNext())
		{
			this.add( (Rule) iter.next());
		}
		add(m_left);
		setLeft(ipcs);
		add(new JButton(m_done_action));
		
		
	}
	
	private void setLeft(int left)
	{
		m_left.setText("Left to spend:" + left);
	}
	
	Action m_done_action = new AbstractAction("Done")
	{
		public void actionPerformed(ActionEvent e)
		{
			s_dialog.setVisible(false);
		}
	};
	
	public IntegerMap getProduction()
	{
		IntegerMap prod = new IntegerMap();
		Iterator iter = m_rules.iterator();
		while(iter.hasNext())
		{
			Rule rule = (Rule) iter.next();
			int quantity = rule.getQuantity();
			if(quantity != 0)
			{
				prod.put(rule.getProductionRule(), quantity);
			}
		}
		return prod;
	}
	
	private void calculateLimits()
	{
		int ipcs = m_id.getResources().getQuantity(Constants.IPCS);
		int spent = 0;
		Iterator iter = m_rules.iterator();
		while(iter.hasNext())
		{
			Rule current = (Rule) iter.next();
			spent += current.getQuantity() * current.getCost();
		}
		int leftToSpend = ipcs - spent;
		setLeft(leftToSpend);
		
		iter = m_rules.iterator();
		while(iter.hasNext())
		{
			Rule current = (Rule) iter.next();
			int max = leftToSpend /  current.getCost();
			max += current.getQuantity();
			current.setMax(max);
		}
		
	}
	
	class Rule extends JPanel
	{
		private ScrollableTextField m_text = new ScrollableTextField(0, Integer.MAX_VALUE);
		private int m_cost;
		private GameData m_data;
		private UnitType m_type;
		private ProductionRule m_rule;

		Rule(ProductionRule rule, GameData data)
		{
			m_data = data;
			m_rule = rule;
			m_cost = rule.getCosts().getInt( m_data.getResourceList().getResource(Constants.IPCS));
			m_type = (UnitType) rule.getResults().keySet().iterator().next();

			this.add( new JLabel(UnitIconImageFactory.instance().getIcon(m_type)));
			this.add(new JLabel( " x " + m_cost));
			this.add(m_text);	
			m_text.addChangeListener(m_listener);
		}
		
		int getCost()
		{
			return m_cost;
		}

		int getQuantity()
		{
			return m_text.getValue();
		}

		ProductionRule getProductionRule()
		{
			return m_rule;
		}
		
		void setMax(int max)
		{
			m_text.setMax(max);
		}
	}
	
	private ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
	{
		public void changedValue(ScrollableTextField stf)
		{
			calculateLimits();
		}	
	};
	
}


