/*
 * Battle.java
 *
 * Created on November 15, 2001, 12:39 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.DelegateBridge;

import games.strategy.triplea.Constants;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Represents a battle.
 */
interface Battle 
{
	
	public void addAttack(Route route, Collection units);
	
	public boolean isBombingRun();
	
	public Territory getTerritory();
			
	public void fight(DelegateBridge bridge);
	
	public void unitsLost(Battle battle, Collection units, DelegateBridge bridge);
	
}