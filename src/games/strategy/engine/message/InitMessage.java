/*
 * InitNodeMessage.java
 *
 * Created on December 27, 2001, 11:58 AM
 */

package games.strategy.engine.message;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author  Sean Bridges
 *
 * When a new MessageManger is added, it recieves this message from all nodes
 * telling it the destinations that each node will accept messages for.
 */
class InitMessage implements Serializable
{
	//A collection of strings
	//use array list since it is serializable
	private ArrayList m_destinations;
	
	private static final long serialVersionUID = 8599273740335248581L;
	
	/** Creates a new instance of InitNodeMessage */
    InitMessage(ArrayList destinations) 
	{
		m_destinations = destinations;
    }
	
	public ArrayList getDestinations()
	{
		return m_destinations;
	}
	
	public String toString()
	{
		return "Init Message:" + m_destinations;
	}
}