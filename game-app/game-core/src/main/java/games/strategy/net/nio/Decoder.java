package games.strategy.net.nio;

import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import games.strategy.net.nio.QuarantineConversation.Action;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.IoUtils;

/** A thread to Decode messages from a reader. */
@Slf4j
class Decoder {
  private final NioReader reader;
  private volatile boolean running = true;
  private final ErrorReporter errorReporter;
  private final IObjectStreamFactory objectStreamFactory;
  private final NioSocket nioSocket;

  /**
   * These sockets are quarantined. They have not logged in, and messages read from them are not
   * passed outside of the quarantine conversation.
   */
  private final ConcurrentHashMap<SocketChannel, QuarantineConversation> quarantine =
      new ConcurrentHashMap<>();

  private final Thread thread;

  Decoder(
      final NioSocket nioSocket,
      final NioReader reader,
      final ErrorReporter reporter,
      final IObjectStreamFactory objectStreamFactory) {
    this.reader = reader;
    errorReporter = reporter;
    this.objectStreamFactory = objectStreamFactory;
    this.nioSocket = nioSocket;
    thread = new Thread(this::loop, "Decoder");
    thread.start();
  }

  void shutDown() {
    running = false;
    thread.interrupt();
  }

  private void loop() {
    while (running) {
      try {
        final SocketReadData data = reader.take();
        if (data == null || !running) {
          continue;
        }

        try {
          final MessageHeader header =
              IoUtils.readFromMemory(
                  data.getData(),
                  is -> {
                    try {
                      return (MessageHeader) objectStreamFactory.create(is).readObject();
                    } catch (final ClassNotFoundException e) {
                      throw new IOException(e);
                    }
                  });
          // make sure we are still open
          final Socket s = data.getChannel().socket();
          if (!running || s == null || s.isInputShutdown()) {
            continue;
          }
          final QuarantineConversation conversation = quarantine.get(data.getChannel());
          if (conversation != null) {
            sendQuarantine(data.getChannel(), conversation, header);
          } else {
            if (nioSocket.getLocalNode() == null) {
              throw new IllegalStateException("we are writing messages, but no local node");
            }
            if (header.getFrom() == null) {
              throw new IllegalArgumentException("Null from: " + header);
            }
            nioSocket.messageReceived(header, data.getChannel());
          }
        } catch (final IOException | RuntimeException e) {
          // we are reading from memory here
          // there should be no network errors, something is odd
          log.error("error reading object", e);
          errorReporter.error(data.getChannel(), e);
        }
      } catch (final InterruptedException e) {
        // Do nothing if we were interrupted due to an explicit shutdown because the thread will
        // terminate normally;
        // otherwise re-interrupt this thread and keep running
        if (running) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private void sendQuarantine(
      final SocketChannel channel,
      final QuarantineConversation conversation,
      final MessageHeader header) {
    final Action a = conversation.message(header.getMessage());
    if (a == Action.TERMINATE) {
      conversation.close();
      // we need to indicate the channel was closed
      errorReporter.error(channel, new CouldNotLogInException());
    } else if (a == Action.UNQUARANTINE) {
      nioSocket.unquarantine(channel, conversation);
      quarantine.remove(channel);
    }
  }

  void add(final SocketChannel channel, final QuarantineConversation conversation) {
    quarantine.put(channel, conversation);
  }

  void close(final SocketChannel channel) {
    // remove if it exists
    final QuarantineConversation conversation = quarantine.remove(channel);
    if (conversation != null) {
      conversation.close();
    }
  }
}
