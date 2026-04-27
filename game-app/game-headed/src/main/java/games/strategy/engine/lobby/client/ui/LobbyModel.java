package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatTransmitter;
import games.strategy.engine.chat.LobbyChatTransmitter;
import games.strategy.engine.lobby.client.login.LoginResult;
import games.strategy.triplea.settings.ClientSetting;
import java.util.function.Consumer;
import lombok.Getter;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;

/**
 * Central domain/network model for a lobby session. Has no Swing dependency. Owns the network
 * connection and all domain objects derived from it; {@link LobbyFrame} receives this and owns only
 * layout.
 */
public class LobbyModel {
  @Getter private final LoginResult loginResult;
  @Getter private final PlayerToLobbyConnection connection;
  @Getter private final LobbyGameListingModel gameListingModel;
  @Getter private final ChatTransmitter chatTransmitter;
  @Getter private final Chat chat;

  public LobbyModel(final LoginResult loginResult, final Consumer<String> onConnectionError) {
    this.loginResult = loginResult;

    connection =
        new PlayerToLobbyConnection(
            ClientSetting.lobbyUri.getValueOrThrow(), loginResult.getApiKey(), onConnectionError);

    gameListingModel = new LobbyGameListingModel(connection);
    chatTransmitter = new LobbyChatTransmitter(connection, loginResult.getUsername());
    chat = new Chat(chatTransmitter);
  }

  void shutdown() {
    chatTransmitter.disconnect();
    connection.close();
  }
}
