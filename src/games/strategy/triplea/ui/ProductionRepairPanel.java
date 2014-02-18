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
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAbilityAttachment;
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
import javax.swing.SwingUtilities;
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
	private static final long serialVersionUID = -6344711064699083729L;
	private JFrame m_owner;
	private JDialog m_dialog;
	private final IUIContext m_uiContext;
	private final List<Rule> m_rules = new ArrayList<Rule>();
	private final JLabel m_left = new JLabel();
	private JButton m_done;
	private PlayerID m_id;
	private boolean m_bid;
	private GameData m_data;
	private static HashMap<Unit, Integer> m_repairCount = new HashMap<Unit, Integer>();
	
	public static HashMap<Unit, IntegerMap<RepairRule>> getProduction(final PlayerID id, final JFrame parent, final GameData data, final boolean bid,
				final HashMap<Unit, IntegerMap<RepairRule>> initialPurchase, final IUIContext uiContext)
	{
		return new ProductionRepairPanel(uiContext).show(id, parent, data, bid, initialPurchase);
	}
	
	/**
	 * Shows the production panel, and returns a map of selected rules.
	 */
	public HashMap<Unit, IntegerMap<RepairRule>> show(final PlayerID id, final JFrame parent, final GameData data, final boolean bid, final HashMap<Unit, IntegerMap<RepairRule>> initialPurchase)
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
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_done.requestFocusInWindow();
			}
		});
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
	
	private void initDialog(final JFrame root)
	{
		m_dialog = new JDialog(root, "Repair", true);
		m_dialog.getContentPane().add(this);
		final Action closeAction = new AbstractAction("")
		{
			private static final long serialVersionUID = 2832491642574528614L;
			
			public void actionPerformed(final ActionEvent e)
			{
				m_dialog.setVisible(false);
			}
		};
		// close the window on escape
		// this is mostly for developers, makes it much easier to quickly cycle through steps
		final KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		final String key = "production.panel.close.prod.popup";
		m_dialog.getRootPane().getActionMap().put(key, closeAction);
		m_dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
	}
	
	/** Creates new ProductionRepairPanel */
	// the constructor can be accessed by subclasses
	public ProductionRepairPanel(final IUIContext uiContext)
	{
		m_uiContext = uiContext;
	}
	
	private void initRules(final PlayerID player, final GameData data, final HashMap<Unit, IntegerMap<RepairRule>> initialPurchase)
	{
		m_data.acquireReadLock();
		try
		{
			m_id = player;
			final CompositeMatchAnd<Unit> myPotentiallyDamagedUnits = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeDamaged);
			final CompositeMatchAnd<Unit> myDamagedUnits = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitHasTakenSomeBombingUnitDamage);
			final Collection<Territory> terrsWithPotentiallyDamagedUnits = Match.getMatches(data.getMap().getTerritories(), Matches.territoryHasUnitsThatMatch(myPotentiallyDamagedUnits));
			for (final RepairRule repairRule : player.getRepairFrontier())
			{
				for (final Territory terr : terrsWithPotentiallyDamagedUnits)
				{
					if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
					{
						for (final Unit u : Match.getMatches(terr.getUnits().getUnits(), myDamagedUnits))
						{
							if (!repairRule.getResults().keySet().iterator().next().equals(u.getType()))
								continue;
							final TripleAUnit taUnit = (TripleAUnit) u;
							final Rule rule = new Rule(repairRule, player, m_uiContext, u);
							// int initialQuantity = initialPurchase.getInt(repairRule);
							int initialQuantity = 0;
							if (initialPurchase.get(u) != null)
								initialQuantity = initialPurchase.get(u).getInt(repairRule);
							// initialQuantity = initialPurchase.get(repairRule).getInt(repairRule);
							rule.setQuantity(initialQuantity);
							rule.setMax(taUnit.getHowMuchCanThisUnitBeRepaired(u, terr));
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
	
	private void initLayout(final PlayerID id)
	{
		final Insets nullInsets = new Insets(0, 0, 0, 0);
		this.removeAll();
		this.setLayout(new GridBagLayout());
		final JLabel legendLabel = new JLabel("Repair Units");
		add(legendLabel, new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));
		for (int x = 0; x < m_rules.size(); x++)
		{
			final boolean even = (x / 2) * 2 == x;
			add(m_rules.get(x), new GridBagConstraints(x / 2, even ? 1 : 2, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, nullInsets, 0, 0));
		}
		add(m_left, new GridBagConstraints(0, 3, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 8, 0, 12), 0, 0));
		m_done = new JButton(m_done_action);
		add(m_done, new GridBagConstraints(0, 4, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 8, 0), 0, 0));
	}
	
	protected void setLeft(final ResourceCollection left)
	{
		final ResourceCollection total = getResources();
		m_left.setText("<html>You have " + left + " left.<br>Out of " + total + "</html>");
	}
	
	Action m_done_action = new AbstractAction("Done")
	{
		private static final long serialVersionUID = 8547016018558520143L;
		
		public void actionPerformed(final ActionEvent e)
		{
			m_dialog.setVisible(false);
		}
	};
	
	private HashMap<Unit, IntegerMap<RepairRule>> getProduction()
	{
		final HashMap<Unit, IntegerMap<RepairRule>> prod = new HashMap<Unit, IntegerMap<RepairRule>>();
		// IntegerMap<RepairRule> repairRule = new IntegerMap<RepairRule>();
		for (final Rule rule : m_rules)
		{
			final int quantity = rule.getQuantity();
			if (quantity != 0)
			{
				final IntegerMap<RepairRule> repairRule = new IntegerMap<RepairRule>();
				final Unit unit = rule.getUnit();
				repairRule.put(rule.getProductionRule(), quantity);
				prod.put(unit, repairRule);
			}
		}
		return prod;
	}
	
	protected void calculateLimits()
	{
		// final IntegerMap<Resource> cost;
		final ResourceCollection resources = getResources();
		final ResourceCollection spent = new ResourceCollection(m_data);
		for (final Rule current : m_rules)
		{
			spent.add(current.getCost(), current.getQuantity());
		}
		final double discount = TechAbilityAttachment.getRepairDiscount(m_id, m_data);
		if (discount != 1.0D)
			spent.discount(discount);
		final ResourceCollection leftToSpend = resources.difference(spent);
		setLeft(leftToSpend);
		for (final Rule current : m_rules)
		{
			int max = leftToSpend.fitsHowOften(current.getCost());
			if (discount != 1.0F)
			{
				max = (int) (max / discount);
			}
			max += current.getQuantity();
			current.setMax(max);
		}
	}
	
	private ResourceCollection getResources()
	{
		if (m_bid)
		{
			// TODO bid only allows you to add PU's to the bid... maybe upgrading Bids so multiple resources can be given? (actually, bids should not cover repairing at all...)
			final String propertyName = m_id.getName() + " bid";
			final int bid = m_data.getProperties().get(propertyName, 0);
			final ResourceCollection bidCollection = new ResourceCollection(m_data);
			m_data.acquireReadLock();
			try
			{
				bidCollection.addResource(m_data.getResourceList().getResource(Constants.PUS), bid);
			} finally
			{
				m_data.releaseReadLock();
			}
			return bidCollection;
		}
		else
			return m_id.getResources();
	}
	
	
	public class Rule extends JPanel
	{
		private static final long serialVersionUID = -6781214135310064908L;
		private final ScrollableTextField m_text = new ScrollableTextField(0, Integer.MAX_VALUE);
		private final IntegerMap<Resource> m_cost;
		private final RepairRule m_rule;
		private final Unit m_unit;
		private final int m_maxRepairAmount;
		private final int m_repairResults;
		
		Rule(final RepairRule rule, final PlayerID id, final IUIContext uiContext, final Unit repairUnit)
		{
			setLayout(new GridBagLayout());
			m_unit = repairUnit;
			m_rule = rule;
			m_cost = rule.getCosts();
			final Territory territoryUnitIsIn = repairUnit.getTerritoryUnitIsIn();
			final UnitType type = (UnitType) rule.getResults().keySet().iterator().next();
			if (!type.equals(repairUnit.getType()))
				throw new IllegalStateException("Rule unit type " + type.getName() + " does not match " + repairUnit.toString() + ".  Please make sure your maps are up to date!");
			m_repairResults = rule.getResults().getInt(type);
			final TripleAUnit taUnit = (TripleAUnit) repairUnit;
			final Icon icon = m_uiContext.getUnitImageFactory().getIcon(type, id, m_data, Matches.UnitHasTakenSomeBombingUnitDamage.match(repairUnit), Matches.UnitIsDisabled.match(repairUnit));
			final String text = "<html> x " + ResourceCollection.toStringForHTML(m_cost) + "</html>";
			final JLabel label = new JLabel(text, icon, SwingConstants.LEFT);
			final JLabel info = new JLabel(territoryUnitIsIn.getName());
			m_maxRepairAmount = taUnit.getHowMuchCanThisUnitBeRepaired(repairUnit, territoryUnitIsIn);
			final JLabel remaining = new JLabel("Damage left to repair: " + m_maxRepairAmount);
			final int space = 8;
			this.add(new JLabel(type.getName()), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 0, 0, 0), 0, 0));
			this.add(label, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space, space), 0, 0));
			this.add(info, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space, space), 0, 0));
			this.add(remaining, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space, space), 0, 0));
			this.add(m_text, new GridBagConstraints(0, 4, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, space, space, space), 0, 0));
			m_text.addChangeListener(m_listener);
			setBorder(new EtchedBorder());
		}
		
		public int getRepairResults()
		{
			return m_repairResults;
		}
		
		IntegerMap<Resource> getCost()
		{
			return m_cost;
		}
		
		public int getQuantity()
		{
			return m_text.getValue();
		}
		
		void setQuantity(final int quantity)
		{
			m_text.setValue(quantity);
		}
		
		RepairRule getProductionRule()
		{
			return m_rule;
		}
		
		void setMax(final int max)
		{
			m_text.setMax((int) (Math.ceil(((double) Math.min(max, m_maxRepairAmount) / (double) m_repairResults))));
		}
		
		public Unit getUnit()
		{
			return m_unit;
		}
	}
	
	private final ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
	{
		public void changedValue(final ScrollableTextField stf)
		{
			calculateLimits();
		}
	};
}
