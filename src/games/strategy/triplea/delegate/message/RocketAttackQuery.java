/*
 * RocketAttackQuery.java
 *
 * Created on December 1, 2001, 10:39 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class RocketAttackQuery extends TerritoryCollectionMessage
{

	/** Creates new RocletAttackQuery */
    public RocketAttackQuery(Collection territories) 
	{
		super(territories);
    }

}
