package games.strategy.net;

import java.io.*;
import java.util.*;

public interface IMessenger
{
	/**
	 *  Send a message to the given node.  Returns immediately.
	 */
	public void send(Serializable msg, INode to);
	/**
	 *  Send a message to all nodes.
	 */
	public void broadcast(Serializable msg);
	/**
	 * Listen for messages of a certain type.
	 */
	public void addMessageListener(IMessageListener listener);
	/**
	 *  Stop listening to messages.
	 */
	public void removeMessageListener(IMessageListener listener);
	/**
	 * Listen for messages of a certain type.
	 */
	public void addErrorListener(IMessengerErrorListener listener);
	/**
	 *  Stop listening to messages.
	 */
	public void removeErrorListener(IMessengerErrorListener listener);
	
	/**
	 * Get the local node
	 */
	public INode getLocalNode();
	
	/**
	 * Get a list of nodes.
	 */
	public Set getNodes();
	/**
	 * test the connection.
	 */
	public boolean isConnected();
	/**
	 * Shut the connection down.
	 */
	public void shutDown();
	
	/**
	 * Add a listener for change in connection status.
	 **/
	public void addConnectionChangeListener(IConnectionChangeListener  listener);
	
	/**
	 * Remove a listener for change in connection status.
	 **/
	public void removeConnectionChangeListener(IConnectionChangeListener  listener) ;
	
	/**
	 * Returns when all messages have been written over the network.
	 * shutdown causes this method to return.
	 * Does not gaurantee that the messages have reached their destination.
	 */
	public void flush();
}
