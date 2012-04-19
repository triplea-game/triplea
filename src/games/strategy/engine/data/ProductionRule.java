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
 * ProductionRule.java
 * 
 * Created on October 13, 2001, 10:05 AM
 */
package games.strategy.engine.data;

import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

import java.io.Serializable;
import java.util.Map.Entry;

/**
 * 
 * @author Sean Bridges
 */
public class ProductionRule extends DefaultNamed implements Serializable
{
	private static final long serialVersionUID = -6598296283127741307L;
	private IntegerMap<Resource> m_cost = new IntegerMap<Resource>();
	private IntegerMap<NamedAttachable> m_results = new IntegerMap<NamedAttachable>();
	
	/** Creates new ProductionRule */
	public ProductionRule(final String name, final GameData data)
	{
		super(name, data);
	}
	
	/** Creates new ProductionRule */
	public ProductionRule(final String name, final GameData data, final IntegerMap<NamedAttachable> results, final IntegerMap<Resource> costs)
	{
		super(name, data);
		m_results = results;
		m_cost = costs;
	}
	
	protected void addCost(final Resource resource, final int quantity)
	{
		m_cost.put(resource, quantity);
	}
	
	/**
	 * Benefits must be a resource or a unit.
	 */
	protected void addResult(final NamedAttachable obj, final int quantity)
	{
		if (!(obj instanceof UnitType) && !(obj instanceof Resource))
			throw new IllegalArgumentException("results must be units or resources, not:" + obj.getClass().getName());
		m_results.put(obj, quantity);
	}
	
	public IntegerMap<Resource> getCosts()
	{
		return m_cost.copy();
	}
	
	public IntegerMap<NamedAttachable> getResults()
	{
		return m_results;
	}
	
	@Override
	public String toString()
	{
		return "ProductionRule:" + getName();
	}
	
	public String toStringCosts()
	{
		final StringBuilder sb = new StringBuilder();
		final Resource pus = getData().getResourceList().getResource(Constants.PUS);
		if (m_cost.getInt(pus) != 0)
		{
			sb.append("; ");
			sb.append(m_cost.getInt(pus));
			sb.append(" " + pus.getName());
		}
		for (final Entry<Resource, Integer> entry : m_cost.entrySet())
		{
			final Resource r = entry.getKey();
			if (r.equals(pus))
				continue;
			final int c = entry.getValue();
			sb.append("; ");
			sb.append(c);
			sb.append(" " + r.getName());
		}
		return sb.toString().replaceFirst("; ", "");
	}
}
