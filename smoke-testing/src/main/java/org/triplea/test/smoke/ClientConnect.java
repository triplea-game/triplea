package org.triplea.test.smoke;

import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.net.ClientMessengerFactory;
import games.strategy.net.IClientMessenger;
import java.net.URI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.util.Version;

/**
 * A simple application to test connectivity to a running bot and lobby. Failure is indicted by
 * throwing an exception which causes a non-zero exit code.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class ClientConnect {
  public static void main(final String[] args) throws Exception {
    connectToLobby();
    connectToBot();
  }

  private static void connectToLobby() {
    log.info("Connecting to lobby...");

    final LobbyLoginClient lobbyLoginClient =
        LobbyLoginClient.newClient(URI.create("http://localhost:8080"));

    final LobbyLoginResponse lobbyLoginResponse = lobbyLoginClient.login("test-user", null);

    if (lobbyLoginResponse.getFailReason() != null) {
      throw new IllegalStateException(
          "Failed to connect to lobby: " + lobbyLoginResponse.getFailReason());
    }
    log.info("Connection to lobby SUCCESSFUL, closing connection");
  }

  private static void connectToBot() throws Exception {
    log.info("Connecting to bot server...");
    final IClientMessenger messenger =
        ClientMessengerFactory.newClientMessenger(
            ClientModel.ClientProps.builder()
                .name("test-user")
                .host("localhost")
                .port(4000)
                .build(),
            new GameObjectStreamFactory(null),
            new ClientLogin(null, new Version(2, 0, 0)));
    Thread.sleep(500L);
    log.info("Connection to bot server SUCCESSFUL, closing connection");
    messenger.shutDown();
  }
}
