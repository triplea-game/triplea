/*
 * TerritoryListener.java
 *
 * Created on November 6, 2001, 2:22 PM
 */

package games.strategy.engine.data.events;

import games.strategy.engine.data.Territory;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface TerritoryListener 
{
	public void unitsChanged(Territory territory);
	public void ownerChanged(Territory territory);
}
