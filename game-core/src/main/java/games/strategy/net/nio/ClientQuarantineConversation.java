package games.strategy.net.nio;

import games.strategy.net.IConnectionLogin;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import lombok.Getter;
import lombok.extern.java.Log;
import org.triplea.java.Interruptibles;
import org.triplea.swing.DialogBuilder;

/** Client-side implementation of {@link QuarantineConversation}. */
@Log
public class ClientQuarantineConversation extends QuarantineConversation {
  private enum Step {
    READ_CHALLENGE,
    READ_ERROR,
    READ_NAMES,
    READ_ADDRESS
  }

  private final IConnectionLogin login;
  private final SocketChannel channel;
  private final NioSocket socket;
  private final CountDownLatch showLatch = new CountDownLatch(1);
  private final CountDownLatch doneShowLatch = new CountDownLatch(1);
  private Step step = Step.READ_CHALLENGE;
  @Getter private String localName;
  @Getter private String serverName;
  @Getter private InetSocketAddress networkVisibleAddress;
  @Getter private InetSocketAddress serverLocalAddress;
  private Map<String, String> challengeProperties;
  private Map<String, String> challengeResponse;
  private volatile boolean isClosed = false;
  @Getter private volatile String errorMessage;

  public ClientQuarantineConversation(
      final IConnectionLogin login,
      final SocketChannel channel,
      final NioSocket socket,
      final String localName,
      final String mac) {
    this.login = login;
    this.localName = localName;
    this.socket = socket;
    this.channel = channel;
    // Send the local name
    send(this.localName);
    // Send the mac address
    send(mac);
  }

  /** Prompts the user to enter their credentials. */
  public void showCredentials() {
    // We need to do this in the thread that created the socket, since the thread that creates the
    // socket will often be,
    // or will block the swing event thread, but the getting of a username/password must be done in
    // the swing event
    // thread. So we have complex code to switch back and forth.
    Interruptibles.await(showLatch);

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
  public Action message(final Serializable serializable) {
    try {
      switch (step) {
        case READ_CHALLENGE:
          // read name, send challenge
          final Map<String, String> challenge = (Map<String, String>) serializable;
          if (challenge != null) {
            challengeProperties = challenge;
            showLatch.countDown();
            Interruptibles.await(doneShowLatch);
            if (isClosed) {
              return Action.NONE;
            }
            send((Serializable) challengeResponse);
          } else {
            showLatch.countDown();
            send(null);
          }
          step = Step.READ_ERROR;
          return Action.NONE;
        case READ_ERROR:
          if (serializable != null) {
            errorMessage = (String) serializable;
            // acknowledge the error
            send(null);
            return Action.TERMINATE;
          }
          step = Step.READ_NAMES;
          return Action.NONE;
        case READ_NAMES:
          final String[] strings = ((String[]) serializable);
          final String assignedName = strings[0];
          // If the assigned name does not start with the desired local name,
          // it means we are already connected and have been given our existing logged in name.
          // We warn the user here to let them know explicitly so it does not look like
          // a silent error.
          if (!assignedName.startsWith(localName)) {
            new Thread(
                    () ->
                        DialogBuilder.builder()
                            .parent(null)
                            .title("Already Logged In")
                            .infoMessage(
                                "<html>Already logged in with another name, "
                                    + "cannot use a different name.<br/>"
                                    + "Logging in as: "
                                    + assignedName)
                            .showDialog())
                .start();
          }
          localName = strings[0];
          serverName = strings[1];
          step = Step.READ_ADDRESS;
          return Action.NONE;
        case READ_ADDRESS:
          // this is the address that others see us as
          final InetSocketAddress[] address = (InetSocketAddress[]) serializable;
          // this is the address the server thinks he is
          networkVisibleAddress = address[0];
          serverLocalAddress = address[1];
          return Action.UNQUARANTINE;
        default:
          throw new IllegalStateException("Invalid state");
      }
    } catch (final Throwable t) {
      isClosed = true;
      showLatch.countDown();
      doneShowLatch.countDown();
      log.log(Level.SEVERE, "error with connection", t);
      return Action.TERMINATE;
    }
  }

  private void send(final Serializable object) {
    // this messenger is quarantined, so to and from dont matter
    final MessageHeader header = new MessageHeader(Node.NULL_NODE, Node.NULL_NODE, object);
    socket.send(channel, header);
  }

  @Override
  public void close() {
    isClosed = true;
    showLatch.countDown();
    doneShowLatch.countDown();
  }
}
