/*
 * Attatchable.java
 *
 * Created on October 14, 2001, 12:46 PM
 */

package games.strategy.engine.data;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface Attatchable 
{
	public void addAttatchment(String key, Attatchment value);
	public Attatchment getAttatchment(String key);
}
