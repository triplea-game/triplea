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
public class MoveMessage implements Message
{
	private Route m_route;
	private Collection m_units;

	/** Creates new BuyMessage */
    public MoveMessage(Collection units, Route route) 
	{
		m_route = route;
		m_units = units;
    }
	
	public Collection getUnits()
	{
		return m_units;
	}
	
	public Route getRoute()
	{
		return m_route;
	}
	
	public String toString()
	{
		return "Move message route:" + m_route + " units:" + m_units;
	}
}
