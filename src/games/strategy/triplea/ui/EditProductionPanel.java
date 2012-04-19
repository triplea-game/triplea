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
 * EditProductionPanel.java
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFrame;

/**
 * 
 * @author Tony Clayton
 * 
 * 
 */
public class EditProductionPanel extends ProductionPanel
{
	private static final long serialVersionUID = 5826523459539469173L;
	
	public static IntegerMap<ProductionRule> getProduction(final PlayerID id, final JFrame parent, final GameData data, final UIContext context)
	{
		return new EditProductionPanel(context).show(id, parent, data, false, new IntegerMap<ProductionRule>());
	}
	
	/** Creates new ProductionPanel */
	private EditProductionPanel(final UIContext uiContext)
	{
		super(uiContext);
	}
	
	protected void setLeft(final int left)
	{
		// no limits, so do nothing here
	}
	
	@Override
	protected void calculateLimits()
	{
		for (final Rule current : getRules())
		{
			current.setMax(99);
		}
	}
	
	@Override
	protected void initRules(final PlayerID player, final GameData data, final IntegerMap<ProductionRule> initialPurchase)
	{
		m_data.acquireReadLock();
		try
		{
			m_id = player;
			final Set<UnitType> unitsAllowed = new HashSet<UnitType>();
			if (player.getProductionFrontier() != null)
			{
				for (final ProductionRule productionRule : player.getProductionFrontier())
				{
					final Rule rule = new Rule(productionRule, player);
					for (final Entry<NamedAttachable, Integer> entry : productionRule.getResults().entrySet())
					{
						if (UnitType.class.isAssignableFrom(entry.getKey().getClass()))
							unitsAllowed.add((UnitType) entry.getKey());
					}
					final int initialQuantity = initialPurchase.getInt(productionRule);
					rule.setQuantity(initialQuantity);
					m_rules.add(rule);
				}
			}
			// this next part is purely to allow people to "add" neutral (null player) units to territories.
			// This is because the null player does not have a production frontier, and we also do not know what units we have art for, so only use the units on a map.
			for (final Territory t : data.getMap())
			{
				for (final Unit u : t.getUnits())
				{
					if (u.getOwner().equals(player))
					{
						final UnitType ut = u.getType();
						if (!unitsAllowed.contains(ut))
						{
							unitsAllowed.add(ut);
							final IntegerMap<NamedAttachable> result = new IntegerMap<NamedAttachable>();
							result.add(ut, 1);
							final IntegerMap<Resource> cost = new IntegerMap<Resource>();
							cost.add(data.getResourceList().getResource(Constants.PUS), 1);
							final ProductionRule newRule = new ProductionRule(ut.getName(), data, result, cost);
							final Rule rule = new Rule(newRule, player);
							rule.setQuantity(0);
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
}
