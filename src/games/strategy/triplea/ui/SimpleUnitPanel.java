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
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.UnitTypeComparator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.util.IntegerMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * A Simple panel that displays a list of units.
 * 
 */
public class SimpleUnitPanel extends JPanel
{
	private static final long serialVersionUID = -3768796793775300770L;
	private final IUIContext m_uiContext;
	
	public SimpleUnitPanel(final IUIContext uiContext)
	{
		m_uiContext = uiContext;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}
	
	/**
	 * 
	 * @param units
	 *            a HashMap in the form ProductionRule -> number of units
	 *            assumes that each production rule has 1 result, which is simple the number of units
	 */
	public void setUnitsFromProductionRuleMap(final IntegerMap<ProductionRule> units, final PlayerID player, final GameData data)
	{
		removeAll();
		final TreeSet<ProductionRule> productionRules = new TreeSet<ProductionRule>(productionRuleComparator);
		productionRules.addAll(units.keySet());
		for (final ProductionRule productionRule : productionRules)
		{
			final int quantity = units.getInt(productionRule);
			final UnitType unit = (UnitType) productionRule.getResults().keySet().iterator().next();
			addUnits(player, data, quantity, unit, false, false);
		}
	}
	
	/**
	 * 
	 * @param units
	 *            a HashMap in the form RepairRule -> number of units
	 *            assumes that each repair rule has 1 result, which is simply the number of units
	 */
	public void setUnitsFromRepairRuleMap(final HashMap<Unit, IntegerMap<RepairRule>> units, final PlayerID player, final GameData data)
	{
		removeAll();
		final Set<Unit> entries = units.keySet();
		for (final Unit unit : entries)
		{
			final IntegerMap<RepairRule> rules = units.get(unit);
			final TreeSet<RepairRule> repairRules = new TreeSet<RepairRule>(repairRuleComparator);
			repairRules.addAll(rules.keySet());
			for (final RepairRule repairRule : repairRules)
			{
				final int quantity = rules.getInt(repairRule);
				if (games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
				{
					// check to see if the repair rule matches the damaged unit
					if (unit.getType().equals((repairRule.getResults().keySet().iterator().next())))
						addUnits(player, data, quantity, unit.getType(), Matches.UnitHasTakenSomeBombingUnitDamage.match(unit), Matches.UnitIsDisabled.match(unit));
				}
			}
		}
	}
	
	/**
	 * 
	 * @param categories
	 *            a collection of UnitCategories
	 */
	public void setUnitsFromCategories(final Collection<UnitCategory> categories, final GameData data)
	{
		removeAll();
		for (final UnitCategory category : categories)
		{
			// TODO Kev determine if we need to identify if the unit is hit/disabled
			addUnits(category.getOwner(), data, category.getUnits().size(), category.getType(), category.hasDamageOrBombingUnitDamage(), category.getDisabled());
		}
	}
	
	private void addUnits(final PlayerID player, final GameData data, final int quantity, final UnitType unit, final boolean damaged, final boolean disabled)
	{
		// TODO Kev determine if we need to identify if the unit is hit/disabled
		final JLabel label = new JLabel();
		label.setText(" x " + quantity);
		label.setIcon(m_uiContext.getUnitImageFactory().getIcon(unit, player, data, damaged, disabled));
		add(label);
	}
	
	Comparator<ProductionRule> productionRuleComparator = new Comparator<ProductionRule>()
	{
		UnitTypeComparator utc = new UnitTypeComparator();
		
		public int compare(final ProductionRule o1, final ProductionRule o2)
		{
			final UnitType u1 = (UnitType) o1.getResults().keySet().iterator().next();
			final UnitType u2 = (UnitType) o2.getResults().keySet().iterator().next();
			return utc.compare(u1, u2);
		}
	};
	Comparator<RepairRule> repairRuleComparator = new Comparator<RepairRule>()
	{
		UnitTypeComparator utc = new UnitTypeComparator();
		
		public int compare(final RepairRule o1, final RepairRule o2)
		{
			final UnitType u1 = (UnitType) o1.getResults().keySet().iterator().next();
			final UnitType u2 = (UnitType) o2.getResults().keySet().iterator().next();
			return utc.compare(u1, u2);
		}
	};
}
