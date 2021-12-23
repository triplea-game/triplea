package games.strategy.net.nio;

import java.nio.channels.SocketChannel;

interface ErrorReporter {
  /**
   * An io error occurred while reading or writing to the socket, and it should be removed from the
   * network.
   */
  void error(SocketChannel channel, Exception e);
}
