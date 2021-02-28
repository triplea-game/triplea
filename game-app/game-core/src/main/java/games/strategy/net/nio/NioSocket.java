package games.strategy.net.nio;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.net.INode;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * The threads needed for a group of sockets using NIO. One thread reds socket data, one thread
 * writes socket data and one thread deserializes (decodes) packets read by the read thread.
 * serializing (encoding) objects to be written across the network is done by threads calling this
 * object.
 */
@Slf4j
public class NioSocket implements ErrorReporter {
  private final Encoder encoder;
  private final Decoder decoder;
  private final NioWriter writer;
  private final NioReader reader;
  private final NioSocketListener listener;

  public NioSocket(final IObjectStreamFactory factory, final NioSocketListener listener) {
    this.listener = listener;
    writer = new NioWriter(this);
    reader = new NioReader(this);
    decoder = new Decoder(this, reader, this, factory);
    encoder = new Encoder(writer, factory);
  }

  INode getLocalNode() {
    return listener.getLocalNode();
  }

  /** Stop our threads. This does not close the sockets we are connected to. */
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
    checkNotNull(to);
    checkNotNull(header);
    checkNotNull(header.getFrom());

    encoder.write(to, header);
  }

  /** Add this channel. The channel will either be unquarantined, or an error will be reported */
  public void add(final SocketChannel channel, final QuarantineConversation conversation) {
    if (channel.isBlocking()) {
      throw new IllegalArgumentException("Channel is blocking");
    }
    // add the decoder first, so it can quarantine the messages!
    decoder.add(channel, conversation);
    reader.add(channel);
  }

  void unquarantine(final SocketChannel channel, final QuarantineConversation conversation) {
    listener.socketUnquarantined(channel, conversation);
  }

  @Override
  public void error(final SocketChannel channel, final Exception e) {
    close(channel);
    listener.socketError(channel, e);
  }

  /** Close the channel, and clean up any data associated with it. */
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
      log.debug("error closing channel", e1);
    }
    decoder.close(channel);
    writer.close(channel);
    reader.close(channel);
  }

  void messageReceived(final MessageHeader header, final SocketChannel channel) {
    listener.messageReceived(header, channel);
  }
}
