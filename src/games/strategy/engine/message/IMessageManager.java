/*
 * IMessageManager.java
 *
 * Created on December 26, 2001, 7:06 PM
 */

package games.strategy.engine.message;

import games.strategy.net.INode;

/**
 *
 * @author  Sean Bridges
 */
public interface IMessageManager 
{
	public void addDestination(IDestination destination);
	public void removeDestination(IDestination destination);
	public Message send(Message msg, String destination);
	public boolean hasDestination(String destination);
}
