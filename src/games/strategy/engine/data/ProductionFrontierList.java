/*
 * ProductionFrontierList.java
 *
 * Created on October 22, 2001, 10:20 AM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ProductionFrontierList extends GameDataComponent
{
	private final Map m_productionFrontiers = new HashMap();
	
    public ProductionFrontierList(GameData data) 
	{
		super(data);
    }
	
	protected void addProductionFrontier(ProductionFrontier pf)
	{
		m_productionFrontiers.put(pf.getName(), pf);
	}
	
	public int size()
	{
		return m_productionFrontiers.size();
	}
	
	public ProductionFrontier getProductionFrontier(String name)
	{
		return (ProductionFrontier) m_productionFrontiers.get(name);
	}
}