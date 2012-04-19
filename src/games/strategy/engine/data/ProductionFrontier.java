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
 * ProductionFrontier.java
 * 
 * Created on October 13, 2001, 10:48 AM
 */
package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class ProductionFrontier extends DefaultNamed implements Iterable<ProductionRule>
{
	private static final long serialVersionUID = -5967251608158552892L;
	private final List<ProductionRule> m_rules = new ArrayList<ProductionRule>();
	private List<ProductionRule> m_cachedRules;
	
	/**
	 * Creates new ProductionFrontier
	 * 
	 * @param name
	 *            name of production frontier
	 * @param data
	 *            game data
	 */
	public ProductionFrontier(final String name, final GameData data)
	{
		super(name, data);
	}
	
	public void addRule(final ProductionRule rule)
	{
		if (m_rules.contains(rule))
			throw new IllegalStateException("Rule already added:" + rule);
		m_rules.add(rule);
		m_cachedRules = null;
	}
	
	public void removeRule(final ProductionRule rule)
	{
		if (!m_rules.contains(rule))
			throw new IllegalStateException("Rule not present:" + rule);
		m_rules.remove(rule);
		m_cachedRules = null;
	}
	
	public List<ProductionRule> getRules()
	{
		if (m_cachedRules == null)
			m_cachedRules = Collections.unmodifiableList(m_rules);
		return m_cachedRules;
	}
	
	public Iterator<ProductionRule> iterator()
	{
		return getRules().iterator();
	}
}
