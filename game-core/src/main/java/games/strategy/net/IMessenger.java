package games.strategy.net;

import java.io.Serializable;

/**
 * A simple way to connect multiple socket end points. An IMessenger listens for incoming messages,
 * and sends them to all registered listeners. Messages are received and sent in order. Note that
 * message listeners are multi threaded, in that they process messages from multiple nodes at the
 * same time, but no more than 1 message from any particular node at a time.
 */
public interface IMessenger {
  /**
   * Send a message to the given node. Returns immediately. If the message cannot be delivered, this
   * method will not throw an exception, but will fail silently.
   */
  void send(Serializable msg, INode to);

  /** Listen for messages. */
  void addMessageListener(IMessageListener listener);

  /** Get the local node. */
  INode getLocalNode();

  /** test the connection. */
  boolean isConnected();

  /** Shut the connection down. */
  void shutDown();

  /**
   * Am I the server node? There should only be one server node, and it should exist before other
   * nodes.
   */
  boolean isServer();

  /** Returns the local node if we are a server node. */
  INode getServerNode();

  /** Add a listener for change in connection status. */
  void addConnectionChangeListener(IConnectionChangeListener listener);

  /** Remove a listener for change in connection status. */
  void removeConnectionChangeListener(IConnectionChangeListener listener);
}
