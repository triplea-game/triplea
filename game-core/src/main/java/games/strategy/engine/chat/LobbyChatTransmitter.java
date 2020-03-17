package games.strategy.engine.chat;

import games.strategy.engine.lobby.connection.PlayerToLobbyConnection;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;

/**
 * Chat transmitter designed to work with lobby, sends and receives messages over Websocket. This
 * class provides a send API and does wiring to connect callbacks from a {@code LobbyChatClient} to
 * a {@code ChatClient}.
 */
@Log
@AllArgsConstructor
public class LobbyChatTransmitter implements ChatTransmitter {
  private final PlayerToLobbyConnection playerToLobbyConnection;
  private final UserName localUserName;

  @Override
  public void setChatClient(final ChatClient chatClient) {
    playerToLobbyConnection.addChatMessageListeners(
        ChatMessageListeners.builder()
            .playerStatusListener(chatClient::statusUpdated)
            .playerLeftListener(chatClient::participantRemoved)
            .playerJoinedListener(chatClient::participantAdded)
            .chatMessageListener(chatClient::messageReceived)
            .connectedListener(chatClient::connected)
            .chatEventListener(chatClient::eventReceived)
            .playerSlappedListener(
                slapEvent -> {
                  if (slapEvent.getSlapped().equals(localUserName)) {
                    chatClient.slappedBy(slapEvent.getSlapper());
                  } else {
                    chatClient.playerSlapped(
                        slapEvent.getSlapper() + " slapped " + slapEvent.getSlapped());
                  }
                })
            .serverErrorListener(log::severe)
            .build());
  }

  @Override
  public Collection<ChatParticipant> connect() {
    return playerToLobbyConnection.connect();
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
