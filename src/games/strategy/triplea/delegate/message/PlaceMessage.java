/*
 * UnitCollectionMessage.java
 *
 * Created on November 8, 2001, 11:36 AM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.data.Territory;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class PlaceMessage implements Message
{
	
	Collection m_units;
	Territory m_to;
	
	/** Creates new UnitCollectionMessage */
    public PlaceMessage(Collection units, Territory to) 
	{
		m_units = units;
		m_to = to;
    }

	public Collection getUnits()
	{
		return m_units;
	}
	
	public Territory getTo()
	{
		return m_to;
	}
	
	public String toString()
	{
		return "PlaceMessage units:" + m_units + " to::" + m_to;
	}
}