/*
 * UnitHolder.java
 *
 * Created on October 26, 2001, 2:27 PM
 */

package games.strategy.engine.data;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface UnitHolder 
{
	public UnitCollection getUnits();
	public void notifyChanged();
}
