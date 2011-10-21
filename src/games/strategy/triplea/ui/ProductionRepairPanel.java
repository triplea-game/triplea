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
 * ProductionRepairPanel.java
 * 
 * Created on November 7, 2001, 10:19 AM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
public class ProductionRepairPanel extends JPanel
{
	
	private JFrame m_owner;
	private JDialog m_dialog;
	private final UIContext m_uiContext;
	
	private List<Rule> m_rules = new ArrayList<Rule>();
	private JLabel m_left = new JLabel();
	private JButton m_done;
	private PlayerID m_id;
	private boolean m_bid;
	private GameData m_data;
	private static HashMap<Unit, Integer> m_repairCount = new HashMap<Unit, Integer>();
	
	public static HashMap<Unit, IntegerMap<RepairRule>> getProduction(PlayerID id, JFrame parent, GameData data, boolean bid, HashMap<Unit, IntegerMap<RepairRule>> initialPurchase, UIContext context)
	{
		return new ProductionRepairPanel(context).show(id, parent, data, bid, initialPurchase);
	}
	
	/**
	 * Shows the production panel, and returns a map of selected rules.
	 */
	public HashMap<Unit, IntegerMap<RepairRule>> show(PlayerID id, JFrame parent, GameData data, boolean bid, HashMap<Unit, IntegerMap<RepairRule>> initialPurchase)
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
	public List<Rule> getRules()
	{
		return this.m_rules;
	};
	
	public static HashMap<Unit, Integer> getUnitRepairs()
	{
		return m_repairCount;
	}
	
	private void initDialog(JFrame root)
	{
		
		m_dialog = new JDialog(root, "Repair", true);
		m_dialog.getContentPane().add(this);
		
		Action closeAction = new AbstractAction("")
		{
			@Override
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
	
	/** Creates new ProductionRepairPanel */
	// the constructor can be accessed by subclasses
	public ProductionRepairPanel(UIContext uiContext)
	{
		m_uiContext = uiContext;
	}
	
	private void initRules(PlayerID player, GameData data, HashMap<Unit, IntegerMap<RepairRule>> initialPurchase)
	{
		m_data.acquireReadLock();
		try
		{
			m_id = player;
			CompositeMatchAnd<Unit> myPotentiallyDamagedUnits = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsFactoryOrCanBeDamaged);
			CompositeMatchAnd<Unit> myDamagedUnits = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitHasSomeUnitDamage());
			Collection<Territory> terrsWithPotentiallyDamagedUnits = Match.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsThatMatch(myPotentiallyDamagedUnits));
			
			for (RepairRule repairRule : player.getRepairFrontier())
			{
				for (Territory terr : terrsWithPotentiallyDamagedUnits)
				{
					if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data))
					{
						TerritoryAttachment ta = TerritoryAttachment.get(terr);
						int unitProduction = ta.getUnitProduction();
						int PUProduction = ta.getProduction();
						
						if (unitProduction < PUProduction)
						{
							for (Unit u : Match.getMatches(terr.getUnits().getUnits(), myPotentiallyDamagedUnits))
							{
								if (!repairRule.getResults().keySet().iterator().next().equals(u.getType()))
									continue;
								Rule rule = new Rule(repairRule, player, m_uiContext, u);
								// int initialQuantity = initialPurchase.getInt(repairRule);
								int initialQuantity = 0;
								if (initialPurchase.get(u) != null)
									initialQuantity = initialPurchase.get(u).getInt(repairRule);
								// initialQuantity = initialPurchase.get(repairRule).getInt(repairRule);
								rule.setQuantity(initialQuantity);
								rule.setMax(PUProduction - unitProduction);
								rule.setUnit(u);
								rule.setName(u.toString());
								m_rules.add(rule);
							}
						}
					}
					else
					// if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
					{
						for (Unit u : Match.getMatches(terr.getUnits().getUnits(), myDamagedUnits))
						{
							if (!repairRule.getResults().keySet().iterator().next().equals(u.getType()))
								continue;
							TripleAUnit taUnit = (TripleAUnit) u;
							Rule rule = new Rule(repairRule, player, m_uiContext, u);
							// int initialQuantity = initialPurchase.getInt(repairRule);
							int initialQuantity = 0;
							if (initialPurchase.get(u) != null)
								initialQuantity = initialPurchase.get(u).getInt(repairRule);
							// initialQuantity = initialPurchase.get(repairRule).getInt(repairRule);
							rule.setQuantity(initialQuantity);
							rule.setMax(taUnit.getHowMuchCanThisUnitBeRepaired(u, terr));
							rule.setUnit(u);
							rule.setName(u.toString());
							m_rules.add(rule);
						}
					}
				}
			}
		} finally
		{
			m_data.releaseReadLock();
		}
		
	}
	
	private void initLayout(PlayerID id)
	{
		Insets nullInsets = new Insets(0, 0, 0, 0);
		this.removeAll();
		this.setLayout(new GridBagLayout());
		JLabel legendLabel = new JLabel("Repair Units");
		add(legendLabel,
					new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));
		
		for (int x = 0; x < m_rules.size(); x++)
		{
			boolean even = (x / 2) * 2 == x;
			add(m_rules.get(x), new GridBagConstraints(x / 2, even ? 1 : 2, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
						nullInsets, 0, 0));
			
		}
		
		add(m_left, new GridBagConstraints(0, 3, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 8, 0, 12), 0, 0));
		m_done = new JButton(m_done_action);
		add(m_done, new GridBagConstraints(0, 4, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
					0, 8, 0), 0, 0));
		
	}
	
	// This method can be overridden by subclasses
	protected void setLeft(int left)
	{
		int total = getPUs();
		//int spent = total - left;
		
		m_left.setText("You have " + left + " " + StringUtil.plural("PU", left) + " left out of " + total + " " + StringUtil.plural("PU", total));
	}
	
	private boolean isIncreasedFactoryProduction(PlayerID player)
	{
		TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.hasIncreasedFactoryProduction();
	}
	
	Action m_done_action = new AbstractAction("Done")
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			m_dialog.setVisible(false);
		}
	};
	
	private HashMap<Unit, IntegerMap<RepairRule>> getProduction()
	{
		HashMap<Unit, IntegerMap<RepairRule>> prod = new HashMap<Unit, IntegerMap<RepairRule>>();
		// IntegerMap<RepairRule> repairRule = new IntegerMap<RepairRule>();
		Iterator<Rule> iter = m_rules.iterator();
		while (iter.hasNext())
		{
			Rule rule = iter.next();
			int quantity = rule.getQuantity();
			if (quantity != 0)
			{
				IntegerMap<RepairRule> repairRule = new IntegerMap<RepairRule>();
				Unit unit = rule.getUnit();
				repairRule.put(rule.getProductionRule(), quantity);
				prod.put(unit, repairRule);
			}
		}
		return prod;
	}
	
	// This method can be overridden by subclasses
	protected void calculateLimits()
	{
		int PUs = getPUs();
		float spent = 0;
		Iterator<Rule> iter = m_rules.iterator();
		while (iter.hasNext())
		{
			Rule current = iter.next();
			spent += current.getQuantity() * current.getCost();
			Unit unit = current.getUnit();
			TripleAUnit taUnit = (TripleAUnit) unit;
			int maxProd = taUnit.getHowMuchCanThisUnitBeRepaired(unit, unit.getTerritoryUnitIsIn());
			current.setMax(maxProd);
		}
		int leftToSpend = (int) (PUs - spent);
		setLeft(leftToSpend);
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
	
	
	@SuppressWarnings("serial")
	public class Rule extends JPanel
	{
		private ScrollableTextField m_text = new ScrollableTextField(0, Integer.MAX_VALUE);
		private float m_cost;
		private RepairRule m_rule;
		private Unit m_unit;
		
		Rule(RepairRule rule, PlayerID id, UIContext uiContext, Unit repairUnit)
		{
			setLayout(new GridBagLayout());
			m_rule = rule;
			m_cost = rule.getCosts().getInt(m_data.getResourceList().getResource(Constants.PUS));
			
			if (isIncreasedFactoryProduction(id))
				m_cost /= 2;
			
			UnitType type = (UnitType) rule.getResults().keySet().iterator().next();
			
			/*if (!type.equals(repairUnit.getType()) && games.strategy.triplea.Properties.getSBRAffectsUnitProduction(m_data))
			{
				// older maps use _hit versions in the results tab.  so, change to non _hit version
				String newType = type.getName();
				if (newType.endsWith("_hit"))
				{
					newType = newType.substring(0, (newType.lastIndexOf("_hit") != -1 ? newType.lastIndexOf("_hit") : newType.length()-1));
					if (m_data.getUnitTypeList().getUnitType(newType) != null)
						type = m_data.getUnitTypeList().getUnitType(newType);
				}
			}*/

			if (!type.equals(repairUnit.getType()))
				throw new IllegalStateException("Rule unit type " + type.getName() + " does not match " + repairUnit.toString() + ".  Please make sure your maps are up to date!");
			
			//UnitAttachment attach = UnitAttachment.get(type);
			TripleAUnit taUnit = (TripleAUnit) repairUnit;
			
			Icon icon;
			if (games.strategy.triplea.Properties.getSBRAffectsUnitProduction(m_data))
				icon = m_uiContext.getUnitImageFactory().getIcon(type, id, m_data, true, false);
			else
				// if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data))
				icon = m_uiContext.getUnitImageFactory().getIcon(type, id, m_data, Matches.UnitHasSomeUnitDamage().match(repairUnit), Matches.UnitIsDisabled().match(repairUnit));
			
			String text = " x " + (m_cost < 10 ? " " : "") + m_cost;
			JLabel label = new JLabel(text, icon, SwingConstants.LEFT);
			JLabel info = new JLabel(repairUnit.getTerritoryUnitIsIn().getName());
			
			int toRepair = taUnit.getHowMuchCanThisUnitBeRepaired(repairUnit, repairUnit.getTerritoryUnitIsIn());
			
			JLabel remaining = new JLabel("Production left to repair: " + toRepair);
			int space = 8;
			this.add(new JLabel(type.getName()), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
						new Insets(2, 0, 0, 0), 0, 0));
			this.add(label, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space,
						space), 0, 0));
			this.add(info, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space,
						space), 0, 0));
			this.add(remaining, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space,
						space), 0, 0));
			
			this.add(m_text, new GridBagConstraints(0, 4, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, space,
						space, space), 0, 0));
			
			m_text.addChangeListener(m_listener);
			setBorder(new EtchedBorder());
		}
		
		float getCost()
		{
			return m_cost;
		}
		
		public int getQuantity()
		{
			return m_text.getValue();
		}
		
		void setQuantity(int quantity)
		{
			m_text.setValue(quantity);
		}
		
		RepairRule getProductionRule()
		{
			return m_rule;
		}
		
		void setMax(int max)
		{
			m_text.setMax(max);
		}
		
		public Unit getUnit()
		{
			return m_unit;
		}
		
		void setUnit(Unit unit)
		{
			m_unit = unit;
		}
		
	}
	
	private ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
	{
		@Override
		public void changedValue(ScrollableTextField stf)
		{
			calculateLimits();
		}
	};
	
}
