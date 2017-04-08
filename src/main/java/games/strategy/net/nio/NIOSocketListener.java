package games.strategy.net.nio;

import java.nio.channels.SocketChannel;

import games.strategy.net.INode;
import games.strategy.net.MessageHeader;

/**
 * Call backs for an NIO Socket.
 */
public interface NIOSocketListener {
  /**
   * This connection will leave quarantine.
   * Messages on this channel will not be read until after this method returns, allowing for setup of the
   * channel.
   */
  void socketUnqaurantined(SocketChannel channel, QuarantineConversation conversation);

  /**
   * An error occured on the channel and it was shut down.
   */
  void socketError(SocketChannel channel, Exception error);

  void messageReceived(MessageHeader message, SocketChannel channel);

  /**
   * Get the remote node id for this channel, or null if the remote node id is not yet known.
   * The node may be unknown if the channel is still quarantined
   */
  INode getRemoteNode(SocketChannel channel);

  /**
   * Get the node id for the local machine, or null if the remote node is not yet known.
   * The node must be known by the time we have an unquarantined channel.
   */
  INode getLocalNode();
}
