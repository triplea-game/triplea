package games.strategy.net.nio;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.net.IConnectionLogin;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;

public class ClientQuarantineConversation extends QuarantineConversation {
  private static final Logger s_logger = Logger.getLogger(ClientQuarantineConversation.class.getName());

  private enum STEP {
    READ_CHALLENGE, READ_ERROR, READ_NAMES, READ_ADDRESS
  }

  private final IConnectionLogin login;
  private final SocketChannel channel;
  private final NIOSocket socket;
  private final CountDownLatch showLatch = new CountDownLatch(1);
  private final CountDownLatch doneShowLatch = new CountDownLatch(1);
  private final String macAddress;
  private STEP step = STEP.READ_CHALLENGE;
  private String localName;
  private String serverName;
  private InetSocketAddress networkVisibleAddress;
  private InetSocketAddress serverLocalAddress;
  private Map<String, String> challengeProperties;
  private Map<String, String> challengeResponse;
  private volatile boolean isClosed = false;
  private volatile String errorMessage;

  public ClientQuarantineConversation(final IConnectionLogin login, final SocketChannel channel, final NIOSocket socket,
      final String localName, final String mac) {
    this.login = login;
    this.localName = localName;
    macAddress = mac;
    this.socket = socket;
    this.channel = channel;
    // Send the local name
    send(this.localName);
    // Send the mac address
    send(macAddress);
  }

  public String getLocalName() {
    return localName;
  }

  public String getMacAddress() {
    return macAddress;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getServerName() {
    return serverName;
  }

  public void showCredentials() {
    /**
     * We need to do this in the thread that created the socket, since
     * the thread that creates the socket will often be, or will block the
     * swing event thread, but the getting of a username/password
     * must be done in the swing event thread.
     * So we have complex code to switch back and forth.
     */
    try {
      showLatch.await();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (login != null && challengeProperties != null) {
      try {
        if (isClosed) {
          return;
        }
        challengeResponse = login.getProperties(challengeProperties);
      } finally {
        doneShowLatch.countDown();
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public ACTION message(final Object o) {
    try {
      switch (step) {
        case READ_CHALLENGE:
          // read name, send challenge
          final Map<String, String> challenge = (Map<String, String>) o;
          if (s_logger.isLoggable(Level.FINER)) {
            s_logger.log(Level.FINER, "read challenge:" + challenge);
          }
          if (challenge != null) {
            challengeProperties = challenge;
            showLatch.countDown();
            try {
              doneShowLatch.await();
            } catch (final InterruptedException e) {
              // ignore
            }
            if (isClosed) {
              return ACTION.NONE;
            }
            if (s_logger.isLoggable(Level.FINER)) {
              s_logger.log(Level.FINER, "writing response" + challengeResponse);
            }
            send((Serializable) challengeResponse);
          } else {
            showLatch.countDown();
            if (s_logger.isLoggable(Level.FINER)) {
              s_logger.log(Level.FINER, "sending null response");
            }
            send(null);
          }
          step = STEP.READ_ERROR;
          return ACTION.NONE;
        case READ_ERROR:
          if (o != null) {
            if (s_logger.isLoggable(Level.FINER)) {
              s_logger.log(Level.FINER, "error:" + o);
            }
            errorMessage = (String) o;
            // acknowledge the error
            send(null);
            return ACTION.TERMINATE;
          }
          step = STEP.READ_NAMES;
          return ACTION.NONE;
        case READ_NAMES:
          final String[] strings = ((String[]) o);
          if (s_logger.isLoggable(Level.FINER)) {
            s_logger.log(Level.FINER, "new local name:" + strings[0]);
          }
          localName = strings[0];
          serverName = strings[1];
          step = STEP.READ_ADDRESS;
          return ACTION.NONE;
        case READ_ADDRESS:
          // this is the adress that others see us as
          final InetSocketAddress[] address = (InetSocketAddress[]) o;
          // this is the address the server thinks he is
          networkVisibleAddress = address[0];
          serverLocalAddress = address[1];
          if (s_logger.isLoggable(Level.FINE)) {
            s_logger.log(Level.FINE, "Server local address:" + serverLocalAddress);
            s_logger.log(Level.FINE, "channel remote address:" + channel.socket().getRemoteSocketAddress());
            s_logger.log(Level.FINE, "network visible address:" + networkVisibleAddress);
            s_logger.log(Level.FINE, "channel local adresss:" + channel.socket().getLocalSocketAddress());
          }
          return ACTION.UNQUARANTINE;
        default:
          throw new IllegalStateException("Invalid state");
      }
    } catch (final Throwable t) {
      isClosed = true;
      showLatch.countDown();
      doneShowLatch.countDown();
      s_logger.log(Level.SEVERE, "error with connection", t);
      return ACTION.TERMINATE;
    }
  }

  private void send(final Serializable object) {
    // this messenger is quarantined, so to and from dont matter
    final MessageHeader header = new MessageHeader(Node.NULL_NODE, Node.NULL_NODE, object);
    socket.send(channel, header);
  }

  public InetSocketAddress getNetworkVisibleSocketAdress() {
    return networkVisibleAddress;
  }

  public InetSocketAddress getServerLocalAddress() {
    return serverLocalAddress;
  }

  @Override
  public void close() {
    isClosed = true;
    showLatch.countDown();
    doneShowLatch.countDown();
  }
}
