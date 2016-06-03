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
 * A thread that writes socket data using NIO .<br>
 * Data is written in packets that are enqued on our buffer.
 * Packets are sent to the sockets in the order that they are received.
 */
public class NIOWriter {
  private static final Logger s_logger = Logger.getLogger(NIOWriter.class.getName());
  private final Selector m_selector;
  private final IErrorReporter m_errorReporter;
  // this is the data we are writing
  private final Map<SocketChannel, List<SocketWriteData>> m_writing =
      new HashMap<>();
  // these are the sockets we arent selecting on, but should now
  private List<SocketChannel> m_socketsToWake = new ArrayList<>();
  // the writing thread and threads adding data to write synchronize on this lock
  private final Object m_mutex = new Object();
  private long m_totalBytes = 0;
  private volatile boolean m_running = true;

  public NIOWriter(final IErrorReporter reporter, final String threadSuffix) {
    m_errorReporter = reporter;
    try {
      m_selector = Selector.open();
    } catch (final IOException e) {
      s_logger.log(Level.SEVERE, "Could not create Selector", e);
      throw new IllegalStateException(e);
    }
    final Thread t = new Thread(() -> {
      loop();
    }, "NIO Writer - " + threadSuffix);
    t.start();
  }

  public void shutDown() {
    m_running = false;
    try {
      m_selector.close();
    } catch (final IOException e) {
      s_logger.log(Level.WARNING, "error closing selector", e);
    }
  }

  private void addNewSocketsToSelector() {
    List<SocketChannel> socketsToWriteCopy;
    synchronized (m_mutex) {
      if (m_socketsToWake.isEmpty()) {
        return;
      }
      socketsToWriteCopy = m_socketsToWake;
      m_socketsToWake = new ArrayList<>();
    }
    for (final SocketChannel channel : socketsToWriteCopy) {
      try {
        channel.register(m_selector, SelectionKey.OP_WRITE);
      } catch (final ClosedChannelException e) {
        s_logger.log(Level.FINEST, "socket already closed", e);
      }
    }
  }

  private void loop() {
    while (m_running) {
      try {
        if (s_logger.isLoggable(Level.FINEST)) {
          s_logger.finest("selecting...");
        }
        try {
          m_selector.select();
        }
        // exceptions can be thrown here, nothing we can do
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4729342
        catch (final Exception e) {
          s_logger.log(Level.INFO, "error reading selection", e);
        }
        if (!m_running) {
          continue;
        }
        // select any new sockets that can be written to
        addNewSocketsToSelector();
        final Set<SelectionKey> selected = m_selector.selectedKeys();
        if (s_logger.isLoggable(Level.FINEST)) {
          s_logger.finest("selected:" + selected.size());
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
                if (s_logger.isLoggable(Level.FINEST)) {
                  s_logger.finest("writing packet:" + packet + " to:" + channel.socket().getRemoteSocketAddress());
                }
                final boolean done = packet.write(channel);
                if (done) {
                  m_totalBytes += packet.size();
                  if (s_logger.isLoggable(Level.FINE)) {
                    String remote = "null";
                    final Socket s = channel.socket();
                    SocketAddress sa = null;
                    if (s != null) {
                      sa = s.getRemoteSocketAddress();
                    }
                    if (sa != null) {
                      remote = sa.toString();
                    }
                    s_logger.log(Level.FINE, " done writing to:" + remote + " size:" + packet.size() + " writeCalls;"
                        + packet.getWriteCalls() + " total:" + m_totalBytes);
                  }
                  removeLast(channel);
                }
              } catch (final Exception e) {
                s_logger.log(Level.FINER, "exception writing", e);
                m_errorReporter.error(channel, e);
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
        s_logger.log(Level.WARNING, "error in writer", e);
      }
    }
  }

  /**
   * Remove the data for this channel
   */
  public void closed(final SocketChannel channel) {
    removeAll(channel);
  }

  private void removeAll(final SocketChannel to) {
    synchronized (m_mutex) {
      m_writing.remove(to);
    }
  }

  private void removeLast(final SocketChannel to) {
    synchronized (m_mutex) {
      final List<SocketWriteData> values = m_writing.get(to);
      if (values == null) {
        s_logger.log(Level.SEVERE, "NO socket data to:" + to + " all:" + values);
        return;
      }
      values.remove(0);
      // remove empty lists, so we can detect that we need to wake up the socket
      if (values.isEmpty()) {
        m_writing.remove(to);
      }
    }
  }

  private SocketWriteData getData(final SocketChannel to) {
    synchronized (m_mutex) {
      if (!m_writing.containsKey(to)) {
        return null;
      }
      final List<SocketWriteData> values = m_writing.get(to);
      if (values.isEmpty()) {
        return null;
      }
      return values.get(0);
    }
  }

  public void enque(final SocketWriteData data, final SocketChannel channel) {
    synchronized (m_mutex) {
      if (!m_running) {
        return;
      }
      if (m_writing.containsKey(channel)) {
        m_writing.get(channel).add(data);
      } else {
        final List<SocketWriteData> values = new ArrayList<>();
        values.add(data);
        m_writing.put(channel, values);
        m_socketsToWake.add(channel);
        m_selector.wakeup();
      }
    }
  }
}
