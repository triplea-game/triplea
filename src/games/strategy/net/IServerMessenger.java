/*
 * IServerMessenger.java
 *
 * Created on December 14, 2001, 1:02 PM
 */

package games.strategy.net;

/**
 *
 * @author  Sean Bridges
 */
public interface IServerMessenger extends IMessenger
{
	public void setAcceptNewConnections(boolean accept);
	public void addConnectionChangeListener(IConnectionChangeListener  listener);
	public void removeConnectionChangeListener(IConnectionChangeListener  listener) ;
}

