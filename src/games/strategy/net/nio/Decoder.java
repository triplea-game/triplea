package games.strategy.net.nio;

import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.message.HubInvocationResults;
import games.strategy.engine.message.HubInvoke;
import games.strategy.engine.message.SpokeInvocationResults;
import games.strategy.engine.message.SpokeInvoke;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.INode;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;
import games.strategy.net.nio.QuarantineConversation.ACTION;

/**
 * A thread to Decode messages from a reader.
 */
public class Decoder {
  private static final Logger logger = Logger.getLogger(Decoder.class.getName());
  private final NIOReader reader;
  private volatile boolean running = true;
  private final IErrorReporter errorReporter;
  private final IObjectStreamFactory objectStreamFactory;
  private final NIOSocket nioSocket;
  /**
   * These sockets are quarantined. They have not logged in, and messages
   * read from them are not passed outside of the quarantine conversation.
   */
  private final ConcurrentHashMap<SocketChannel, QuarantineConversation> quarantine =
      new ConcurrentHashMap<>();
  private final Thread thread;

  public Decoder(final NIOSocket nioSocket, final NIOReader reader, final IErrorReporter reporter,
      final IObjectStreamFactory objectStreamFactory, final String threadSuffix) {
    this.reader = reader;
    errorReporter = reporter;
    this.objectStreamFactory = objectStreamFactory;
    this.nioSocket = nioSocket;
    thread = new Thread(() -> loop(), "Decoder -" + threadSuffix);
    thread.start();
  }

  public void shutDown() {
    running = false;
    thread.interrupt();
  }

  private void loop() {
    while (running) {
      try {
        SocketReadData data;
        try {
          data = reader.take();
        } catch (final InterruptedException e) {
          continue;
        }
        if (data == null || !running) {
          continue;
        }
        if (logger.isLoggable(Level.FINEST)) {
          logger.finest("Decoding packet:" + data);
        }
        final ByteArrayInputStream stream = new ByteArrayInputStream(data.getData());
        try {
          final MessageHeader header = readMessageHeader(data.getChannel(), objectStreamFactory.create(stream));
          if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "header decoded:" + header);
          }
          // make sure we are still open
          final Socket s = data.getChannel().socket();
          if (!running || s == null || s.isInputShutdown()) {
            continue;
          }
          final QuarantineConversation converstation = quarantine.get(data.getChannel());
          if (converstation != null) {
            sendQuarantine(data.getChannel(), converstation, header);
          } else {
            if (nioSocket.getLocalNode() == null) {
              throw new IllegalStateException("we are writing messages, but no local node");
            }
            if (header.getFrom() == null) {
              throw new IllegalArgumentException("Null from:" + header);
            }
            if (logger.isLoggable(Level.FINER)) {
              logger.log(Level.FINER, "decoded  msg:" + header.getMessage() + " size:" + data.size());
            }
            nioSocket.messageReceived(header, data.getChannel());
          }
        } catch (final Exception ioe) {
          // we are reading from memory here
          // there should be no network errors, something
          // is odd
          logger.log(Level.SEVERE, "error reading object", ioe);
          errorReporter.error(data.getChannel(), ioe);
        }
      } catch (final Exception e) {
        // catch unhandles exceptions to that the decoder
        // thread doesnt die
        logger.log(Level.WARNING, "error in decoder", e);
      }
    }
  }

  private void sendQuarantine(final SocketChannel channel, final QuarantineConversation conversation,
      final MessageHeader header) {
    final ACTION a = conversation.message(header.getMessage());
    if (a == ACTION.TERMINATE) {
      if (logger.isLoggable(Level.FINER)) {
        logger.log(Level.FINER, "Terminating quarantined connection to:" + channel.socket().getRemoteSocketAddress());
      }
      conversation.close();
      // we need to indicate the channel was closed
      errorReporter.error(channel, new CouldNotLogInException());
    } else if (a == ACTION.UNQUARANTINE) {
      if (logger.isLoggable(Level.FINER)) {
        logger.log(Level.FINER, "Accepting quarantined connection to:" + channel.socket().getRemoteSocketAddress());
      }
      nioSocket.unquarantine(channel, conversation);
      quarantine.remove(channel);
    }
  }

  private MessageHeader readMessageHeader(final SocketChannel channel, final ObjectInputStream objectInput)
      throws IOException, ClassNotFoundException {
    INode to;
    if (objectInput.read() == 1) {
      to = null;
    } else {
      if (objectInput.read() == 1) {
        // this may be null if we
        // have not yet fully joined the network
        to = nioSocket.getLocalNode();
      } else {
        to = new Node();
        ((Node) to).readExternal(objectInput);
      }
    }
    INode from;
    final int readMark = objectInput.read();
    if (readMark == 1) {
      from = nioSocket.getRemoteNode(channel);
    } else if (readMark == 2) {
      from = null;
    } else {
      from = new Node();
      ((Node) from).readExternal(objectInput);
    }
    Serializable message;
    final byte type = (byte) objectInput.read();
    if (type != Byte.MAX_VALUE) {
      final Externalizable template = getTemplate(type);
      template.readExternal(objectInput);
      message = template;
    } else {
      message = (Serializable) objectInput.readObject();
    }
    return new MessageHeader(to, from, message);
  }

  private static Externalizable getTemplate(final byte type) {
    switch (type) {
      case 1:
        return new HubInvoke();
      case 2:
        return new SpokeInvoke();
      case 3:
        return new HubInvocationResults();
      case 4:
        return new SpokeInvocationResults();
      default:
        throw new IllegalStateException("not recognized, " + type);
    }
  }

  /**
   * Most messages we pass will be one of the types below
   * since each of these is externalizable, we can
   * reduce network traffic considerably by skipping the
   * writing of the full identifiers, and simply write a single
   * byte to show the type.
   */
  public static byte getType(final Object msg) {
    if (msg instanceof HubInvoke) {
      return 1;
    } else if (msg instanceof SpokeInvoke) {
      return 2;
    } else if (msg instanceof HubInvocationResults) {
      return 3;
    } else if (msg instanceof SpokeInvocationResults) {
      return 4;
    }
    return Byte.MAX_VALUE;
  }

  public void add(final SocketChannel channel, final QuarantineConversation conversation) {
    quarantine.put(channel, conversation);
  }

  public void closed(final SocketChannel channel) {
    // remove if it exists
    final QuarantineConversation conversation = quarantine.remove(channel);
    if (conversation != null) {
      conversation.close();
    }
  }
}
