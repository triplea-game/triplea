/*
 * ProductionFrontier.java
 *
 * Created on October 13, 2001, 10:48 AM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ProductionFrontier extends DefaultNamed
{
	
	private final Set m_rules = new HashSet();
	private Set m_cachedRules;
	
	/** Creates new ProductionFrontier */
    public ProductionFrontier(String name, GameData data) 
	{
		super(name, data);
    }
		
	public void addRule(ProductionRule rule)
	{
		m_rules.add(rule);
		m_cachedRules = null;
	}
	
	public Set getRules()
	{
		if(m_cachedRules == null)
			m_cachedRules = Collections.unmodifiableSet(m_rules);
		return m_cachedRules;
	}
}
