/*
 * ProductionRule.java
 *
 * Created on October 13, 2001, 10:05 AM
 */

package games.strategy.engine.data;

import java.util.*;
import java.io.Serializable;

import games.strategy.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ProductionRule extends DefaultNamed implements Serializable
{
	private static final long serialVersionUID = -1198201375488904892L;
	
	private IntegerMap m_cost = new IntegerMap();
	private IntegerMap m_results = new IntegerMap();
	
	/** Creates new ProductionRule */
    public ProductionRule(String name, GameData data) 
	{
		super(name, data);
    }

	protected void addCost(Resource resource, int quantity)
	{
		m_cost.put(resource, quantity);
	}
	
	/** 
	 * Benefits must be a resource or a unit.
	 */
	protected void addResult(Object obj, int quantity)
	{
		if(! (obj instanceof UnitType) && ! (obj instanceof Resource))
			throw new IllegalArgumentException("results must be units or resources, not:" + obj.getClass().getName() );
		
		m_results.put(obj, quantity);
	}	
	
	public IntegerMap getCosts()
	{
		return m_cost.copy();
	}
	
	public IntegerMap getResults()
	{
		return m_results.copy();
	}
	
	public String toString()
	{
		return "ProductionRule:" + getName();
	}
}