/*
 * ProductionRuleList.java
 *
 * Created on October 22, 2001, 10:23 AM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ProductionRuleList extends GameDataComponent
{
	private final Map m_productionRules = new HashMap();
	
    public ProductionRuleList(GameData data) 
	{
		super(data);
    }
	
	protected void addProductionRule(ProductionRule pf)
	{
		m_productionRules.put(pf.getName(), pf);
	}
	
	public int size()
	{
		return m_productionRules.size();
	}
	
	public ProductionRule getProductionRule(String name)
	{
		return (ProductionRule) m_productionRules.get(name);
	}
}