package games.strategy.engine.chat;

import games.strategy.engine.lobby.client.LobbyClient;
import java.util.Collection;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.http.client.lobby.chat.LobbyChatClient;

/**
 * Chat transmitter designed to work with lobby, sends and receives messages over Websocket. This
 * class provides a send API and does wiring to connect callbacks from a {@code LobbyChatClient} to
 * a {@code ChatClient}.
 */
// TODO: Project#12 test-me
public class LobbyChatTransmitter implements ChatTransmitter {
  private final LobbyChatClient lobbyChatClient;
  private final PlayerName localPlayerName;

  public LobbyChatTransmitter(final LobbyClient lobbyClient) {
    this.localPlayerName = lobbyClient.getPlayerName();
    this.lobbyChatClient = lobbyClient.getHttpLobbyClient().getLobbyChatClient();
  }

  @Override
  public void setChatClient(final ChatClient chatClient) {
    lobbyChatClient.addPlayerStatusListener(chatClient::statusUpdated);
    lobbyChatClient.addPlayerLeftListener(chatClient::participantRemoved);
    lobbyChatClient.addPlayerJoinedListener(chatClient::participantAdded);
    lobbyChatClient.addChatMessageListener(chatClient::messageReceived);
    lobbyChatClient.addConnectedListener(chatClient::connected);
    lobbyChatClient.addChatEventListener(chatClient::eventReceived);
    lobbyChatClient.addPlayerSlappedListener(
        slapEvent -> {
          if (slapEvent.getSlapped().equals(localPlayerName)) {
            chatClient.slappedBy(slapEvent.getSlapper());
          } else {
            chatClient.playerSlapped(slapEvent.getSlapper() + " slapped " + slapEvent.getSlapped());
          }
        });
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
  public void slap(final PlayerName playerName) {
    lobbyChatClient.slapPlayer(playerName);
  }

  @Override
  public void updateStatus(final String status) {
    lobbyChatClient.updateStatus(status);
  }

  @Override
  public PlayerName getLocalPlayerName() {
    return localPlayerName;
  }
}
