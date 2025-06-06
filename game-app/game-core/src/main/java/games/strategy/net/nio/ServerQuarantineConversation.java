package games.strategy.net.nio;

import games.strategy.net.ILoginValidator;
import games.strategy.net.MessageHeader;
import games.strategy.net.Node;
import games.strategy.net.ServerMessenger;
import games.strategy.net.UserNameAssigner;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;

/** Server-side implementation of {@link QuarantineConversation}. */
@Slf4j
public class ServerQuarantineConversation extends QuarantineConversation {
  /*
   * Communication sequence
   * 1) server reads client name
   * 2) server sends challenge (or null if no challenge is to be made)
   * 3) server reads response (or null if no challenge)
   * 4) server send null then client name and node info on success, or an error message
   * if there is an error
   * 5) if the client reads an error message, the client sends an acknowledgment (we need to
   * make sur the client gets the message before closing the socket).
   */
  private enum Step {
    READ_NAME,
    READ_MAC,
    CHALLENGE,
    ACK_ERROR
  }

  private final ILoginValidator validator;
  private final SocketChannel channel;
  private final NioSocket socket;
  private Step step = Step.READ_NAME;
  @Getter private String remoteName;
  private String remoteMac;
  private Map<String, String> challenge;
  private final ServerMessenger serverMessenger;

  public ServerQuarantineConversation(
      final ILoginValidator validator,
      final SocketChannel channel,
      final NioSocket socket,
      final ServerMessenger serverMessenger) {
    this.validator = validator;
    this.socket = socket;
    this.channel = channel;
    this.serverMessenger = serverMessenger;
  }

  @Override
  public Action message(final Serializable serializable) {
    try {
      switch (step) {
        case READ_NAME:
          remoteName = ((String) serializable).trim();
          step = Step.READ_MAC;
          return Action.NONE;
        case READ_MAC:
          remoteMac = (String) serializable;
          if (validator != null) {
            challenge = validator.getChallengeProperties(remoteName);
          }
          send((Serializable) challenge);
          step = Step.CHALLENGE;
          return Action.NONE;
        case CHALLENGE:
          @SuppressWarnings("unchecked")
          final Map<String, String> response = (Map<String, String>) serializable;
          String error = null;

          if (validator != null) {
            error =
                Optional.ofNullable(
                        validator.verifyConnection(
                            challenge,
                            response,
                            remoteName,
                            remoteMac,
                            (InetSocketAddress) channel.socket().getRemoteSocketAddress()))
                    .orElseGet(() -> UserName.validate(remoteName).orElse(null));
            if (error != null) {
              step = Step.ACK_ERROR;
              send(error);
              return Action.NONE;
            } else {
              send(null);
            }
          } else {
            send(null);
          }

          synchronized (serverMessenger.newNodeLock) {
            // aggregate all player names by mac address (there can be multiple names per mac
            // address)
            final Collection<String> names = serverMessenger.getPlayerNames();
            remoteName = UserNameAssigner.assignName(remoteName, remoteMac, names);
          }

          // send the node its assigned name, our name, an error message that could contain a magic
          // string informing client they should reset their password, and last an API key that can
          // be used for further http server interaction.
          send(new String[] {remoteName, serverMessenger.getLocalNode().getName(), error});

          // send the node its and our address as we see it
          send(
              new InetSocketAddress[] {
                (InetSocketAddress) channel.socket().getRemoteSocketAddress(),
                serverMessenger.getLocalNode().getSocketAddress()
              });

          // Login succeeded, so notify the ServerMessenger about the login with the name, mac, etc.
          serverMessenger.notifyPlayerLogin(UserName.of(remoteName), remoteMac);
          // We are good
          return Action.UNQUARANTINE;
        case ACK_ERROR:
          return Action.TERMINATE;
        default:
          throw new IllegalStateException("Invalid state");
      }
    } catch (final Throwable t) {
      log.error("Error with connection", t);
      return Action.TERMINATE;
    }
  }

  private void send(final Serializable object) {
    // this messenger is quarantined, so to and from don't matter
    final MessageHeader header = new MessageHeader(Node.NULL_NODE, Node.NULL_NODE, object);
    socket.send(channel, header);
  }

  @Override
  public void close() {}
}
