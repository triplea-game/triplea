package games.strategy.net.nio;

import java.io.IOException;
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
import lombok.extern.slf4j.Slf4j;

/**
 * A thread that reads socket data using NIO from a collection of sockets. Data is read in packets,
 * and placed in the output queue. Packets are placed in the output queue in order they are read
 * from the socket.
 */
@Slf4j
class NioReader {
  private final LinkedBlockingQueue<SocketReadData> outputQueue = new LinkedBlockingQueue<>();
  private volatile boolean running = true;
  private final Map<SocketChannel, SocketReadData> reading = new ConcurrentHashMap<>();
  private final ErrorReporter errorReporter;
  private final Selector selector;
  private final Object socketsToAddMutex = new Object();
  private final List<SocketChannel> socketsToAdd = new ArrayList<>();

  NioReader(final ErrorReporter reporter) {
    errorReporter = reporter;
    try {
      selector = Selector.open();
    } catch (final IOException e) {
      log.error("Could not create Selector", e);
      throw new IllegalStateException(e);
    }
    new Thread(this::loop, "NIO Reader").start();
  }

  void shutDown() {
    running = false;
    try {
      selector.close();
    } catch (final Exception e) {
      log.warn("error closing selector", e);
    }
  }

  void add(final SocketChannel channel) {
    synchronized (socketsToAddMutex) {
      socketsToAdd.add(channel);
      selector.wakeup();
    }
  }

  private void selectNewChannels() {
    final List<SocketChannel> toAdd;
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
        selector.select();
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
                enque(packet);
              }
            } catch (final Exception e) {
              log.debug("exception reading", e);
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
        // catch unhandled exceptions so that the reader thread doesn't die
        log.error("error in reader", e);
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

  void close(final SocketChannel channel) {
    reading.remove(channel);
  }
}
