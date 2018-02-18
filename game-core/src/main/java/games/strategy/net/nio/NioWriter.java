package games.strategy.net.nio;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread that writes socket data using NIO .
 * Data is written in packets that are enqueued on our buffer.
 * Packets are sent to the sockets in the order that they are received.
 */
class NioWriter {
  private static final Logger logger = Logger.getLogger(NioWriter.class.getName());
  private final Selector selector;
  private final ErrorReporter errorReporter;
  // this is the data we are writing
  private final Map<SocketChannel, List<SocketWriteData>> writing = new HashMap<>();
  // these are the sockets we arent selecting on, but should now
  private List<SocketChannel> socketsToWake = new ArrayList<>();
  // the writing thread and threads adding data to write synchronize on this lock
  private final Object mutex = new Object();
  private long totalBytes = 0;
  private volatile boolean running = true;

  NioWriter(final ErrorReporter reporter, final String threadSuffix) {
    errorReporter = reporter;
    try {
      selector = Selector.open();
    } catch (final IOException e) {
      logger.log(Level.SEVERE, "Could not create Selector", e);
      throw new IllegalStateException(e);
    }
    new Thread(this::loop, "NIO Writer - " + threadSuffix).start();
  }

  void shutDown() {
    running = false;
    try {
      selector.close();
    } catch (final IOException e) {
      logger.log(Level.WARNING, "error closing selector", e);
    }
  }

  private void addNewSocketsToSelector() {
    final List<SocketChannel> socketsToWriteCopy;
    synchronized (mutex) {
      if (socketsToWake.isEmpty()) {
        return;
      }
      socketsToWriteCopy = socketsToWake;
      socketsToWake = new ArrayList<>();
    }
    for (final SocketChannel channel : socketsToWriteCopy) {
      try {
        channel.register(selector, SelectionKey.OP_WRITE);
      } catch (final ClosedChannelException e) {
        logger.log(Level.FINEST, "socket already closed", e);
      }
    }
  }

  private void loop() {
    while (running) {
      try {
        if (logger.isLoggable(Level.FINEST)) {
          logger.finest("selecting...");
        }
        try {
          // exceptions can be thrown here, nothing we can do
          // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4729342
          selector.select();
        } catch (final Exception e) {
          logger.log(Level.SEVERE, "error reading selection", e);
        }
        if (!running) {
          continue;
        }
        // select any new sockets that can be written to
        addNewSocketsToSelector();
        final Set<SelectionKey> selected = selector.selectedKeys();
        if (logger.isLoggable(Level.FINEST)) {
          logger.finest("selected:" + selected.size());
        }
        final Iterator<SelectionKey> iter = selected.iterator();
        while (iter.hasNext()) {
          final SelectionKey key = iter.next();
          iter.remove();
          if (key.isValid() && key.isWritable()) {
            final SocketChannel channel = (SocketChannel) key.channel();
            final SocketWriteData packet = getData(channel);
            if (packet != null) {
              try {
                if (logger.isLoggable(Level.FINEST)) {
                  logger.finest("writing packet:" + packet + " to:" + channel.socket().getRemoteSocketAddress());
                }
                final boolean done = packet.write(channel);
                if (done) {
                  totalBytes += packet.size();
                  if (logger.isLoggable(Level.FINE)) {
                    String remote = "null";
                    final Socket s = channel.socket();
                    SocketAddress sa = null;
                    if (s != null) {
                      sa = s.getRemoteSocketAddress();
                    }
                    if (sa != null) {
                      remote = sa.toString();
                    }
                    logger.log(Level.FINE, " done writing to:" + remote + " size:" + packet.size() + " writeCalls;"
                        + packet.getWriteCalls() + " total:" + totalBytes);
                  }
                  removeLast(channel);
                }
              } catch (final Exception e) {
                logger.log(Level.FINER, "exception writing", e);
                errorReporter.error(channel, e);
                key.cancel();
              }
            } else {
              // nothing to write
              // cancel the key, otherwise we will
              // spin forever as the socket will always be writable
              key.cancel();
            }
          }
        }
      } catch (final Exception e) {
        // catch unhandles exceptions to that the writer
        // thread doesnt die
        logger.log(Level.WARNING, "error in writer", e);
      }
    }
  }

  /**
   * Remove the data for this channel.
   */
  void closed(final SocketChannel channel) {
    removeAll(channel);
  }

  private void removeAll(final SocketChannel to) {
    synchronized (mutex) {
      writing.remove(to);
    }
  }

  private void removeLast(final SocketChannel to) {
    synchronized (mutex) {
      final List<SocketWriteData> values = writing.get(to);
      if (values == null) {
        logger.log(Level.SEVERE, "NO socket data to:" + to + " all:" + values);
        return;
      }
      values.remove(0);
      // remove empty lists, so we can detect that we need to wake up the socket
      if (values.isEmpty()) {
        writing.remove(to);
      }
    }
  }

  private SocketWriteData getData(final SocketChannel to) {
    synchronized (mutex) {
      if (!writing.containsKey(to)) {
        return null;
      }
      final List<SocketWriteData> values = writing.get(to);
      if (values.isEmpty()) {
        return null;
      }
      return values.get(0);
    }
  }

  void enque(final SocketWriteData data, final SocketChannel channel) {
    synchronized (mutex) {
      if (!running) {
        return;
      }
      if (writing.containsKey(channel)) {
        writing.get(channel).add(data);
      } else {
        final List<SocketWriteData> values = new ArrayList<>();
        values.add(data);
        writing.put(channel, values);
        socketsToWake.add(channel);
        selector.wakeup();
      }
    }
  }
}
