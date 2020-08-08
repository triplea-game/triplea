package games.strategy.net.nio;

import games.strategy.net.INode;
import games.strategy.net.MessageHeader;
import java.nio.channels.SocketChannel;

/** Call backs for an NIO Socket. */
public interface NioSocketListener {
  /**
   * This connection will leave quarantine. Messages on this channel will not be read until after
   * this method returns, allowing for setup of the channel.
   */
  void socketUnquarantined(SocketChannel channel, QuarantineConversation conversation);

  /** An error occurred on the channel and it was shut down. */
  void socketError(SocketChannel channel, Exception error);

  void messageReceived(MessageHeader message, SocketChannel channel);

  /**
   * Get the node id for the local machine, or null if the remote node is not yet known. The node
   * must be known by the time we have an unquarantined channel.
   */
  INode getLocalNode();
}
