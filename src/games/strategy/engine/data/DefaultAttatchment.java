/*
 * Attatchment.java
 *
 * Created on November 8, 2001, 3:09 PM
 */

package games.strategy.engine.data;



/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class DefaultAttatchment implements Attatchment
{

	private GameData m_data;
	
	/**
	 * Throws an error if format is invalid.
	 */
	public static int getInt(String aString)
	{
		int val = 0;
		try
		{
			val = Integer.parseInt(aString);
		} catch( NumberFormatException nfe)
		{
			throw new IllegalArgumentException(aString + " is not a valid int value");
		}
		return val;
	}
	
	/**
	 * Throws an error if format is invalid.  Must be either true or false ignoring case.
	 */
	public static boolean getBool(String aString)
	{
		if(aString.equalsIgnoreCase("true") )
			return true;
		else if(aString.equalsIgnoreCase("false"))
			return false;
		else
			throw new IllegalArgumentException(aString + " is not a valid boolean");
	}
	
	public void setData(GameData data) 
	{
		m_data = data;
	}
	
	protected GameData getData()
	{
		return m_data;
	}
	
	/**
	 * Called after the attatchment is created.
	 */
	public void validate() throws GameParseException
	{
	}
	
	/** Creates new Attatchment */
    public DefaultAttatchment() 
	{
		
    }

}
