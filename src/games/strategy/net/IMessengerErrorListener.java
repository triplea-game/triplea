/*
 * MessengerErrorListener.java
 *
 * Created on December 12, 2001, 10:46 AM
 */

package games.strategy.net;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface IMessengerErrorListener 
{
	/**
	 * The connection to the given node has been lost.
	 * unsent contains the messages that have not been sent.
	 */
	public void connectionLost(INode node, Exception reason, List unsent);

	/**
	 * The messenger is no longer able to send or receive messages.
	 * This signals that an error has occured, will not be sent if the 
	 * node was shutdown.
	 * Unsent contains the messages that have not been sent.
	 */
	public void messengerInvalid(IMessenger messenger, Exception reason, List unsent);
}
