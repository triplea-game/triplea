/*
 * DestinationChangeMessage.java
 * 
 * Created on December 27, 2001, 11:43 AM
 */
package games.strategy.engine.message;

/**
 * 
 * @author Sean Bridges
 * 
 *         A destination has been added or removed
 */
class DestinationChangeMessage implements java.io.Serializable
{
	private final boolean m_add;
	private final String m_destination;
	private static final long serialVersionUID = -168782943218162839L;
	
	/** Creates a new instance of ManagerStateMessage */
	DestinationChangeMessage(final String destination, final boolean add)
	{
		m_add = add;
		m_destination = destination;
	}
	
	public boolean isAdd()
	{
		return m_add;
	}
	
	public String getDestination()
	{
		return m_destination;
	}
}
