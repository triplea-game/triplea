package games.strategy.engine.chat;

import games.strategy.engine.lobby.client.LobbyClient;
import java.util.Collection;
import lombok.extern.java.Log;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;
import org.triplea.http.client.lobby.chat.LobbyChatClient;

/**
 * Chat transmitter designed to work with lobby, sends and receives messages over Websocket. This
 * class provides a send API and does wiring to connect callbacks from a {@code LobbyChatClient} to
 * a {@code ChatClient}.
 */
// TODO: Project#12 test-me
@Log
public class LobbyChatTransmitter implements ChatTransmitter {
  private final LobbyChatClient lobbyChatClient;
  private final UserName localUserName;

  public LobbyChatTransmitter(final LobbyClient lobbyClient) {
    this.localUserName = lobbyClient.getUserName();
    this.lobbyChatClient = lobbyClient.getHttpLobbyClient().getLobbyChatClient();
  }

  @Override
  public void setChatClient(final ChatClient chatClient) {
    lobbyChatClient.setChatMessageListeners(
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
    return lobbyChatClient.connect();
  }

  @Override
  public void disconnect() {
    lobbyChatClient.close();
  }

  @Override
  public void sendMessage(final String message) {
    lobbyChatClient.sendChatMessage(message);
  }

  @Override
  public void slap(final UserName userName) {
    lobbyChatClient.slapPlayer(userName);
  }

  @Override
  public void updateStatus(final String status) {
    lobbyChatClient.updateStatus(status);
  }

  @Override
  public UserName getLocalUserName() {
    return localUserName;
  }
}
