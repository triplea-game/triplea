/*
 * MustMoveWithQuery.java
 *
 * Created on December 3, 2001, 6:20 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.engine.message.Message;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Gives a territory and a collection of units, and asks
 * what units must move with the given units.
 * All units are either carriers or transports.
 * All units are owned by the player whose current turn it is.
 * 
 *
 */
public class MustMoveWithQuery implements Message
{

	private Territory m_start;
	private Collection m_units;
	
	/** Creates new DependentUnitsQuery */
    public MustMoveWithQuery(Collection units, Territory start) 
	{
		m_start = start;
		m_units = units;
    }
	
	public Territory getStart()
	{
		return m_start;
	}
	
	public Collection getUnits()
	{
		return m_units;
	}
}