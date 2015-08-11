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
public class NIOSocket implements IErrorReporter {
  private static final Logger s_logger = Logger.getLogger(NIOSocket.class.getName());
  private final Encoder m_encoder;
  private final Decoder m_decoder;
  private final NIOWriter m_writer;
  private final NIOReader m_reader;
  private final NIOSocketListener m_listener;

  public NIOSocket(final IObjectStreamFactory factory, final NIOSocketListener listener, final String name) {
    m_listener = listener;
    m_writer = new NIOWriter(this, name);
    m_reader = new NIOReader(this, name);
    m_decoder = new Decoder(this, m_reader, this, factory, name);
    m_encoder = new Encoder(this, m_writer, factory);
  }

  INode getLocalNode() {
    return m_listener.getLocalNode();
  }

  INode getRemoteNode(final SocketChannel channel) {
    return m_listener.getRemoteNode(channel);
  }

  /**
   * Stop our threads.
   * This does not close the sockets we are connected to.
   */
  public void shutDown() {
    m_writer.shutDown();
    m_reader.shutDown();
    m_decoder.shutDown();
  }

  public void send(final SocketChannel to, final MessageHeader header) {
    if (to == null) {
      throw new IllegalArgumentException("to cant be null!");
    }
    if (header == null) {
      throw new IllegalArgumentException("header cant be null");
    }
    m_encoder.write(to, header);
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
    m_decoder.add(channel, conversation);
    m_reader.add(channel);
  }

  void unquarantine(final SocketChannel channel, final QuarantineConversation conversation) {
    m_listener.socketUnqaurantined(channel, conversation);
  }

  @Override
  public void error(final SocketChannel channel, final Exception e) {
    close(channel);
    m_listener.socketError(channel, e);
  }

  /**
   * Close the channel, and clean up any data associated with it
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
      s_logger.log(Level.FINE, "error closing channel", e1);
    }
    m_decoder.closed(channel);
    m_writer.closed(channel);
    m_reader.closed(channel);
  }

  void messageReceived(final MessageHeader header, final SocketChannel channel) {
    m_listener.messageReceived(header, channel);
  }
}
