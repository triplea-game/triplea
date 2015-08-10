package games.strategy.net;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * A simple way to connect multiple socket end points.
 * An IMessenger listens for incoming messages, and sends
 * them to all registered listeners.
 *
 * Messages are recieved and sent in order. Note that message listeners are multi threaded, in
 * that they process messages from multiple nodes at the same time, but no more than 1 message
 * from any particular node at a time.
 *
 *
 */
public interface IMessenger {
  /**
   * Send a message to the given node. Returns immediately.
   *
   * If the message cannot be delivered, this method will not throw an exception, but will
   * fail silently.
   */
  public void send(Serializable msg, INode to);

  /**
   * Send a message to all nodes.
   */
  public void broadcast(Serializable msg);

  /**
   * Listen for messages.
   */
  public void addMessageListener(IMessageListener listener);

  /**
   * Stop listening to messages.
   */
  public void removeMessageListener(IMessageListener listener);

  /**
   * Listen for errors
   */
  public void addErrorListener(IMessengerErrorListener listener);

  /**
   * Stop listening for errors.
   */
  public void removeErrorListener(IMessengerErrorListener listener);

  /**
   * Get the local node
   */
  public INode getLocalNode();

  /**
   * test the connection.
   */
  public boolean isConnected();

  /**
   * Shut the connection down.
   */
  public void shutDown();

  /**
   * Am I the server node?
   * There should only be one server node, and it should exist before other nodes.
   */
  public boolean isServer();

  /**
   *
   * @return local node if we are a server node.
   */
  public INode getServerNode();

  /**
   * Get the socket address to which we talk to the server.
   * This may be different than getServerNode().getSocketAddress() since
   * the server will report the socket that he thinks the server is running on,
   * if the server is behind a firewall, or a NAT, then this socket will be
   * different than the actual port we use.
   */
  public InetSocketAddress getRemoteServerSocketAddress();

  // TODO: add this into next release: boolean isShutDown();
}
