/*
 * IServerMessageListener.java
 *
 * Created on December 14, 2001, 1:03 PM
 */

package games.strategy.net;

/**
 *
 * @author  Sean Bridges
 */
public interface IConnectionChangeListener 
{
	/**
	 * The number of connections has changed.
	 */
	public void connectionsChanged();
}
