/*
 * Attatchment.java
 *
 * Created on November 8, 2001, 3:09 PM
 */

package games.strategy.engine.data;

import java.io.Serializable;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface Attatchment extends Serializable
{
	public void setData(GameData m_data);
	
	/**
	 * Called after the attatchment is created.
	 * IF an error occurs should throw an 
	 * exception to halt the parsing
	 */
	public void validate() throws GameParseException;
	
}
