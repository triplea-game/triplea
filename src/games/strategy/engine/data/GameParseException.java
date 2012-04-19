/*
 * GameParseException.java
 * 
 * Created on October 22, 2001, 8:54 AM
 */
package games.strategy.engine.data;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class GameParseException extends Exception
{
	private static final long serialVersionUID = 4015574053053781872L;
	
	public GameParseException(final String error)
	{
		super(error);
	}
}
