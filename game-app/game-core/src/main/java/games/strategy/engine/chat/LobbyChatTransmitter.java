package games.strategy.engine.chat;

import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatEventReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatterListingMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerJoinedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerLeftMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerSlapReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerStatusUpdateReceivedMessage;

/**
 * Chat transmitter designed to work with lobby, sends and receives messages over Websocket. This
 * class provides a send API and does wiring to connect callbacks from a {@code LobbyChatClient} to
 * a {@code ChatClient}.
 */
@AllArgsConstructor
public class LobbyChatTransmitter implements ChatTransmitter {
  private final PlayerToLobbyConnection playerToLobbyConnection;
  private final UserName localUserName;

  @Override
  public void setChatClient(final ChatClient chatClient) {
    playerToLobbyConnection.addMessageListener(
        ChatterListingMessage.TYPE, message -> chatClient.connected(message.getChatters()));

    playerToLobbyConnection.addMessageListener(
        PlayerStatusUpdateReceivedMessage.TYPE,
        message -> chatClient.statusUpdated(message.getUserName(), message.getStatus()));

    playerToLobbyConnection.addMessageListener(
        PlayerLeftMessage.TYPE, message -> chatClient.participantRemoved(message.getUserName()));

    playerToLobbyConnection.addMessageListener(
        PlayerJoinedMessage.TYPE,
        message -> chatClient.participantAdded(message.getChatParticipant()));

    playerToLobbyConnection.addMessageListener(
        ChatReceivedMessage.TYPE,
        message -> chatClient.messageReceived(message.getSender(), message.getMessage()));

    playerToLobbyConnection.addMessageListener(
        ChatEventReceivedMessage.TYPE, message -> chatClient.eventReceived(message.getMessage()));

    playerToLobbyConnection.addMessageListener(
        PlayerSlapReceivedMessage.TYPE, message -> handleSlapMessage(chatClient, message));

    playerToLobbyConnection.addConnectionResetListener(
        playerToLobbyConnection::sendConnectToChatMessage);

    playerToLobbyConnection.sendConnectToChatMessage();
  }

  private void handleSlapMessage(
      final ChatClient chatClient, final PlayerSlapReceivedMessage message) {
    if (message.getSlappedPlayer().equals(localUserName)) {
      chatClient.slappedBy(message.getSlappingPlayer());
    } else {
      chatClient.eventReceived(
          message.getSlappingPlayer() + " slapped " + message.getSlappedPlayer());
    }
  }

  @Override
  public Collection<ChatParticipant> connect() {
    return List.of();
  }

  @Override
  public void disconnect() {
    playerToLobbyConnection.close();
  }

  @Override
  public void sendMessage(final String message) {
    playerToLobbyConnection.sendChatMessage(message);
  }

  @Override
  public void slap(final UserName userName) {
    playerToLobbyConnection.slapPlayer(userName);
  }

  @Override
  public void updateStatus(final String status) {
    playerToLobbyConnection.updateStatus(status);
  }

  @Override
  public UserName getLocalUserName() {
    return localUserName;
  }
}
