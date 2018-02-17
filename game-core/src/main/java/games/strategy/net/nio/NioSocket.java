package games.strategy.net.nio;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.net.INode;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;

/**
 * The threads needed for a group of sockets using NIO.
 * One thread reds socket data, one thread writes socket data
 * and one thread deserializes (decodes) packets read by the read
 * thread.
 * serializing (encoding) objects to be written across the network is done
 * by threads calling this object.
 */
public class NioSocket implements ErrorReporter {
  private static final Logger logger = Logger.getLogger(NioSocket.class.getName());
  private final Encoder encoder;
  private final Decoder decoder;
  private final NioWriter writer;
  private final NioReader reader;
  private final NioSocketListener listener;

  public NioSocket(final IObjectStreamFactory factory, final NioSocketListener listener, final String name) {
    this.listener = listener;
    writer = new NioWriter(this, name);
    reader = new NioReader(this, name);
    decoder = new Decoder(this, reader, this, factory, name);
    encoder = new Encoder(this, writer, factory);
  }

  INode getLocalNode() {
    return listener.getLocalNode();
  }

  INode getRemoteNode(final SocketChannel channel) {
    return listener.getRemoteNode(channel);
  }

  /**
   * Stop our threads.
   * This does not close the sockets we are connected to.
   */
  public void shutDown() {
    writer.shutDown();
    reader.shutDown();
    decoder.shutDown();
  }

  /**
   * Sends the specified message header through the specified channel.
   *
   * @param to The destination channel.
   * @param header The message header to send.
   */
  public void send(final SocketChannel to, final MessageHeader header) {
    if (to == null) {
      throw new IllegalArgumentException("to cant be null!");
    }
    if (header == null) {
      throw new IllegalArgumentException("header cant be null");
    }
    encoder.write(to, header);
  }

  /**
   * Add this channel.
   * The channel will either be unquarantined, or an error will be reported
   */
  public void add(final SocketChannel channel, final QuarantineConversation conversation) {
    if (channel.isBlocking()) {
      throw new IllegalArgumentException("Channel is blocking");
    }
    // add the decoder first, so it can quarantine the messages!
    decoder.add(channel, conversation);
    reader.add(channel);
  }

  void unquarantine(final SocketChannel channel, final QuarantineConversation conversation) {
    listener.socketUnqaurantined(channel, conversation);
  }

  @Override
  public void error(final SocketChannel channel, final Exception e) {
    close(channel);
    listener.socketError(channel, e);
  }

  /**
   * Close the channel, and clean up any data associated with it.
   */
  public void close(final SocketChannel channel) {
    try {
      final Socket s = channel.socket();
      if (!s.isInputShutdown()) {
        s.shutdownInput();
      }
      if (!s.isOutputShutdown()) {
        s.shutdownOutput();
      }
      if (!s.isClosed()) {
        s.close();
      }
      channel.close();
    } catch (final IOException e1) {
      logger.log(Level.FINE, "error closing channel", e1);
    }
    decoder.closed(channel);
    writer.closed(channel);
    reader.closed(channel);
  }

  void messageReceived(final MessageHeader header, final SocketChannel channel) {
    listener.messageReceived(header, channel);
  }
}
