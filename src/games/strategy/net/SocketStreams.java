package games.strategy.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * A holder for all the streams associated with a socket.
 * <p>
 * We need this because we can only buffer a socket once (otherwise you have issues when the first buffer reads bytes that weren't meant for
 * it)
 * <p>
 */
public class SocketStreams {
  private final InputStream m_socketIn;
  private final OutputStream m_socketOut;
  private final BufferedOutputStream m_bufferedOut;
  private final BufferedInputStream m_bufferedIn;

  public SocketStreams(final Socket s) throws IOException {
    m_socketIn = s.getInputStream();
    m_socketOut = s.getOutputStream();
    m_bufferedIn = new BufferedInputStream(m_socketIn);
    m_bufferedOut = new BufferedOutputStream(m_socketOut);
  }

  public BufferedInputStream getBufferedIn() {
    return m_bufferedIn;
  }

  public BufferedOutputStream getBufferedOut() {
    return m_bufferedOut;
  }

  public InputStream getSocketIn() {
    return m_socketIn;
  }

  public OutputStream getSocketOut() {
    return m_socketOut;
  }
}
