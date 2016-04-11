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
 * We need this because we can only buffer a socket once (otherwise you have issues when the first buffer reads bytes
 * that weren't meant for
 * it)
 * <p>
 */
public class SocketStreams {
  private final InputStream socketIn;
  private final OutputStream socketOut;
  private final BufferedOutputStream bufferedOut;
  private final BufferedInputStream bufferedIn;

  public SocketStreams(final Socket s) throws IOException {
    socketIn = s.getInputStream();
    socketOut = s.getOutputStream();
    bufferedIn = new BufferedInputStream(socketIn);
    bufferedOut = new BufferedOutputStream(socketOut);
  }

  public BufferedInputStream getBufferedIn() {
    return bufferedIn;
  }

  public BufferedOutputStream getBufferedOut() {
    return bufferedOut;
  }

  public InputStream getSocketIn() {
    return socketIn;
  }

  public OutputStream getSocketOut() {
    return socketOut;
  }
}
