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

/*
 * ProductionPanel.java
 * 
 * Created on November 7, 2001, 10:19 AM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.IntegerMap;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * 
 */
public class ProductionPanel extends JPanel
{
	
	private JFrame m_owner;
	private JDialog m_dialog;
	
	// Edwin: made these protected so the class can be extended
	protected final UIContext m_uiContext;
	protected List<Rule> m_rules = new ArrayList<Rule>();
	protected JLabel m_left = new JLabel();
	protected JButton m_done;
	protected PlayerID m_id;
	private boolean m_bid;
	private GameData m_data;
	
	public static IntegerMap<ProductionRule> getProduction(PlayerID id, JFrame parent, GameData data, boolean bid, IntegerMap<ProductionRule> initialPurchase, UIContext context)
	{
		return new ProductionPanel(context).show(id, parent, data, bid, initialPurchase);
	}
	
	/**
	 * Shows the production panel, and returns a map of selected rules.
	 */
	public IntegerMap<ProductionRule> show(PlayerID id, JFrame parent, GameData data, boolean bid, IntegerMap<ProductionRule> initialPurchase)
	{
		if (!(parent == m_owner))
			m_dialog = null;
		
		if (m_dialog == null)
			initDialog(parent);
		
		this.m_bid = bid;
		this.m_data = data;
		this.initRules(id, data, initialPurchase);
		this.initLayout(id);
		this.calculateLimits();
		
		m_dialog.pack();
		m_dialog.setLocationRelativeTo(parent);
		m_done.requestFocusInWindow();
		m_dialog.setVisible(true);
		
		m_dialog.dispose();
		
		return getProduction();
	}
	
	// this method can be accessed by subclasses
	protected List<Rule> getRules()
	{
		return m_rules;
	};
	
	private void initDialog(JFrame root)
	{
		
		m_dialog = new JDialog(root, "Produce", true);
		m_dialog.getContentPane().add(this);
		
		Action closeAction = new AbstractAction("")
		{
			
			public void actionPerformed(ActionEvent e)
			{
				m_dialog.setVisible(false);
			}
		};
		
		// close the window on escape
		// this is mostly for developers, makes it much easier to quickly cycle through steps
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		
		String key = "production.panel.close.prod.popup";
		
		m_dialog.getRootPane().getActionMap().put(key, closeAction);
		m_dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
	}
	
	/** Creates new ProductionPanel */
	// the constructor can be accessed by subclasses
	protected ProductionPanel(UIContext uiContext)
	{
		m_uiContext = uiContext;
	}
	
	private void initRules(PlayerID player, GameData data, IntegerMap<ProductionRule> initialPurchase)
	{
		m_data.acquireReadLock();
		try
		{
			m_id = player;
			
			for (ProductionRule productionRule : player.getProductionFrontier())
			{
				Rule rule = new Rule(productionRule, player);
				int initialQuantity = initialPurchase.getInt(productionRule);
				rule.setQuantity(initialQuantity);
				m_rules.add(rule);
			}
		} finally
		{
			m_data.releaseReadLock();
		}
	}
	
	// Edwin: made this protected so the class can be extended
	protected void initLayout(PlayerID id)
	{
		Insets nullInsets = new Insets(0, 0, 0, 0);
		this.removeAll();
		this.setLayout(new GridBagLayout());
		JLabel legendLabel = new JLabel("Attack/Defense/Movement");
		add(legendLabel,
					new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));
		
		int rows = m_rules.size() / 7;
		rows = Math.max(2, rows);
		
		for (int x = 0; x < m_rules.size(); x++)
		{
			add(m_rules.get(x).getPanelComponent(), new GridBagConstraints(x / rows, (x % rows) + 1, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH,
						nullInsets, 0, 0));
		}
		
		int startY = m_rules.size() / rows;
		add(m_left, new GridBagConstraints(0, startY + 1, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 8, 0, 12), 0, 0));
		m_done = new JButton(m_done_action);
		add(m_done, new GridBagConstraints(0, startY + 2, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
					0, 8, 0), 0, 0));
	}
	
	// This method can be overridden by subclasses
	protected void setLeft(int left, int totalUnits)
	{
		int total = getPUs();
		int spent = total - left;
		
		m_left.setText("You have " + left + " " + StringUtil.plural("PU", spent) + " left out of " + total + " " + StringUtil.plural("PU", total) + ", Purchasing a total of " + totalUnits + " units.");
	}
	
	Action m_done_action = new AbstractAction("Done")
	{
		
		public void actionPerformed(ActionEvent e)
		{
			m_dialog.setVisible(false);
		}
	};
	
	private IntegerMap<ProductionRule> getProduction()
	{
		IntegerMap<ProductionRule> prod = new IntegerMap<ProductionRule>();
		Iterator<Rule> iter = m_rules.iterator();
		while (iter.hasNext())
		{
			Rule rule = iter.next();
			int quantity = rule.getQuantity();
			if (quantity != 0)
			{
				prod.put(rule.getProductionRule(), quantity);
			}
		}
		return prod;
	}
	
	// This method can be overridden by subclasses
	protected void calculateLimits()
	{
		int PUs = getPUs();
		int spent = 0;
		int totalUnits = 0;
		Iterator<Rule> iter = m_rules.iterator();
		while (iter.hasNext())
		{
			Rule current = iter.next();
			spent += current.getQuantity() * current.getCost();
			totalUnits += current.getQuantity() * current.getProductionRule().getResults().totalValues();
		}
		int leftToSpend = PUs - spent;
		setLeft(leftToSpend, totalUnits);
		
		iter = m_rules.iterator();
		while (iter.hasNext())
		{
			Rule current = iter.next();
			int max = leftToSpend / current.getCost();
			max += current.getQuantity();
			current.setMax(max);
		}
	}
	
	private int getPUs()
	{
		if (m_bid)
		{
			String propertyName = m_id.getName() + " bid";
			return Integer.parseInt(m_data.getProperties().get(propertyName).toString());
		}
		else
			return m_id.getResources().getQuantity(Constants.PUS);
	}
	
	
	class Rule
	{
		private int m_cost;
		private int m_quantity;
		private ProductionRule m_rule;
		private PlayerID m_id;
		private Set<ScrollableTextField> m_textFields = new HashSet<ScrollableTextField>();
		
		protected JPanel getPanelComponent()
		{
			JPanel panel = new JPanel();
			/*String eol = "  ";
			try {
				eol = System.getProperty("line.separator");
			} catch (Exception e) { }*/
			ScrollableTextField i_text = new ScrollableTextField(0, Integer.MAX_VALUE);
			i_text.setValue(m_quantity);
			panel.setLayout(new GridBagLayout());
			UnitType type = (UnitType) m_rule.getResults().keySet().iterator().next();
			Icon icon = m_uiContext.getUnitImageFactory().getIcon(type, m_id, m_data, false, false);
			UnitAttachment attach = UnitAttachment.get(type);
			int attack = attach.getAttack(m_id);
			int movement = attach.getMovement(m_id);
			int defense = attach.getDefense(m_id);
			int numberOfUnitsGiven = m_rule.getResults().totalValues();
			String text;
			if (numberOfUnitsGiven > 1)
				text = "<html> x " + (m_cost < 10 ? " " : "") + m_cost + "<br>" + "for " + numberOfUnitsGiven + "<br>" + " units</html>";
			else
				text = " x " + (m_cost < 10 ? " " : "") + m_cost;
			JLabel label = new JLabel(text, icon, SwingConstants.LEFT);
			JLabel info = new JLabel(attack + "/" + defense + "/" + movement);
			// info.setToolTipText(" attack:" + attack + " defense :" + defense +" movement:" +movement);
			String toolTipText = "<html>" + type.getName() + ": " + type.getTooltip(m_id, true) + "</html>";
			info.setToolTipText(toolTipText);
			label.setToolTipText(toolTipText);
			int space = 8;
			JLabel name = new JLabel(type.getName());
			// change name color for 'upgrades and consumes' unit types
			if (attach.getConsumesUnits() != null && attach.getConsumesUnits().totalValues() == 1)
				name.setForeground(Color.CYAN);
			else if (attach.getConsumesUnits() != null && attach.getConsumesUnits().totalValues() > 1)
				name.setForeground(Color.BLUE);
			
			panel.add(name, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(2, 0, 0, 0), 0, 0));
			panel.add(label, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space,
						space), 0, 0));
			panel.add(info, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space,
						space), 0, 0));
			
			panel.add(i_text, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, space,
						space, space), 0, 0));
			i_text.addChangeListener(m_listener);
			m_textFields.add(i_text);
			panel.setBorder(new EtchedBorder());
			
			return panel;
		}
		
		Rule(ProductionRule rule, PlayerID id)
		{
			m_rule = rule;
			m_cost = rule.getCosts().getInt(m_data.getResourceList().getResource(Constants.PUS));
			m_id = id;
		}
		
		int getCost()
		{
			return m_cost;
		}
		
		int getQuantity()
		{
			return m_quantity;
		}
		
		void setQuantity(int quantity)
		{
			m_quantity = quantity;
			for (ScrollableTextField textField : m_textFields)
			{
				if (textField.getValue() != quantity)
					textField.setValue(quantity);
			}
		}
		
		ProductionRule getProductionRule()
		{
			return m_rule;
		}
		
		void setMax(int max)
		{
			for (ScrollableTextField textField : m_textFields)
			{
				textField.setMax(max);
			}
		}
		
		private ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
		{
			
			public void changedValue(ScrollableTextField stf)
			{
				if (stf.getValue() != m_quantity)
				{
					m_quantity = stf.getValue();
					calculateLimits();
					for (ScrollableTextField textField : m_textFields)
					{
						if (!stf.equals(textField))
							textField.setValue(m_quantity);
					}
				}
			}
		};
	}
	
}
