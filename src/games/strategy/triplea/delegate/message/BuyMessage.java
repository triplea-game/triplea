/*
 * BuyMessage.java
 *
 * Created on November 6, 2001, 8:26 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.message.*;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class BuyMessage implements Message
{
	
	private static final long serialVersionUID = -982799945814358613L;
	
	IntegerMap m_buy = new IntegerMap();
	
	/** Creates new BuyMessage */
    public BuyMessage(IntegerMap map) 
	{
		m_buy.add(map);
    }
	
	public void set(ProductionRule rule, int quantity)
	{
		m_buy.put(rule, new Integer(quantity));
	}
	
	public IntegerMap getPurchase()
	{
		return m_buy;
	}
	
	public String toString()
	{
		return "Buy Message:" + m_buy;
	}
}
