/*
 * TestAttatchment.java
 *
 * Created on October 22, 2001, 7:32 PM
 */

package games.strategy.engine.xml;

import games.strategy.engine.data.GameData;

/**
 *
 * @author  Sean Bridges
 * @version 
 */
public class TestAttatchment implements games.strategy.engine.data.Attatchment {

	private String m_value;
	
	
	/** Creates new TestAttatchment */
    public TestAttatchment() 
	{
    }
	
	public void setValue(String value)
	{
		m_value = value;
	}
	
	public String getValue()
	{
		return m_value;
	}

	public void setData(GameData m_data) 
	{
	}
	
	/**
	 * Called after the attatchment is created.
	 * IF an error occurs should throw a runtime
	 * exception to halt the vm.
	 */
	public void validate() {
	}
	
}
