package games.strategy.net.nio;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread that reads socket data using NIO from a collection of sockets.
 * Data is read in packets, and placed in the output queue.
 * Packets are placed in the output queue in order they are read from the socket.
 */
class NioReader {
  private static final Logger logger = Logger.getLogger(NioReader.class.getName());
  private final LinkedBlockingQueue<SocketReadData> outputQueue = new LinkedBlockingQueue<>();
  private volatile boolean running = true;
  private final Map<SocketChannel, SocketReadData> reading = new ConcurrentHashMap<>();
  private final ErrorReporter errorReporter;
  private final Selector selector;
  private final Object socketsToAddMutex = new Object();
  private final List<SocketChannel> socketsToAdd = new ArrayList<>();
  private long totalBytes;

  NioReader(final ErrorReporter reporter, final String threadSuffix) {
    errorReporter = reporter;
    try {
      selector = Selector.open();
    } catch (final IOException e) {
      logger.log(Level.SEVERE, "Could not create Selector", e);
      throw new IllegalStateException(e);
    }
    new Thread(this::loop, "NIO Reader - " + threadSuffix).start();
  }

  void shutDown() {
    running = false;
    try {
      selector.close();
    } catch (final Exception e) {
      logger.log(Level.WARNING, "error closing selector", e);
    }
  }

  void add(final SocketChannel channel) {
    synchronized (socketsToAddMutex) {
      socketsToAdd.add(channel);
      selector.wakeup();
    }
  }

  private void selectNewChannels() {
    List<SocketChannel> toAdd = null;
    synchronized (socketsToAddMutex) {
      if (socketsToAdd.isEmpty()) {
        return;
      }
      toAdd = new ArrayList<>(socketsToAdd);
      socketsToAdd.clear();
    }
    for (final SocketChannel channel : toAdd) {
      try {
        channel.register(selector, SelectionKey.OP_READ);
      } catch (final ClosedChannelException e) {
        // this is ok, the channel is closed, so dont bother reading it
        return;
      }
    }
  }

  private void loop() {
    while (running) {
      try {
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
        selectNewChannels();
        final Set<SelectionKey> selected = selector.selectedKeys();
        final Iterator<SelectionKey> iter = selected.iterator();
        while (iter.hasNext()) {
          final SelectionKey key = iter.next();
          iter.remove();
          if (key.isValid() && key.isReadable()) {
            final SocketChannel channel = (SocketChannel) key.channel();
            final SocketReadData packet = getReadData(channel);
            try {
              final boolean done = packet.read(channel);
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
                  logger.log(Level.FINE, " done reading from:" + remote + " size:" + packet.size() + " readCalls;"
                      + packet.getReadCalls() + " total:" + totalBytes);
                }
                enque(packet);
              }
            } catch (final Exception e) {
              logger.log(Level.FINER, "exception reading", e);
              key.cancel();
              errorReporter.error(channel, e);
            }
          } else if (!key.isValid()) {
            final SocketChannel channel = (SocketChannel) key.channel();
            key.cancel();
            errorReporter.error(channel, new SocketException("triplea:key cancelled"));
          }
        }
      } catch (final Exception e) {
        // catch unhandles exceptions to that the reader
        // thread doesnt die
        logger.log(Level.SEVERE, "error in reader", e);
      }
    }
  }

  private void enque(final SocketReadData packet) {
    reading.remove(packet.getChannel());
    outputQueue.offer(packet);
  }

  private SocketReadData getReadData(final SocketChannel channel) {
    if (reading.containsKey(channel)) {
      return reading.get(channel);
    }
    final SocketReadData packet = new SocketReadData(channel);
    reading.put(channel, packet);
    return packet;
  }

  SocketReadData take() throws InterruptedException {
    return outputQueue.take();
  }

  void closed(final SocketChannel channel) {
    reading.remove(channel);
  }
}
