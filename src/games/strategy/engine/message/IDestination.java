/*
 * IDestination.java
 *
 * Created on December 26, 2001, 7:02 PM
 */

package games.strategy.engine.message;

/**
 *
 * @author  Sean Bridges
 */
public interface IDestination 
{
	public Message sendMessage(Message message);
	public String getName();

}
