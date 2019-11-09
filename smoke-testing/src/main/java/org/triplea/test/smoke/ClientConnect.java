package org.triplea.test.smoke;

import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.net.ClientMessengerFactory;
import games.strategy.net.IClientMessenger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.live.servers.ServerProperties;

/**
 * A simple application to test connectivity to a running bot and lobby. Failure is indicted by
 * throwing an exception which causes a non-zero exit code.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Log
public final class ClientConnect {
  public static void main(final String[] args) throws Exception {
    connectToLobby();
    connectToBot();
  }

  private static void connectToLobby() throws Exception {
    log.info("Connecting to lobby...");
    final IClientMessenger messenger =
        ClientMessengerFactory.newAnonymousUserMessenger(
            ServerProperties.builder().host("localhost").port(3304).httpsPort(5432).build(),
            "test-user");
    Thread.sleep(500L);
    log.info("Connection to lobby SUCCESSFUL, closing connection");
    messenger.shutDown();
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
            new ClientLogin(null));
    Thread.sleep(500L);
    log.info("Connection to bot server SUCCESSFUL, closing connection");
    messenger.shutDown();
  }
}
