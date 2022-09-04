package org.triplea.game.server;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.triplea.web.socket.StandaloneWebsocketServer;
import org.triplea.web.socket.WebSocketMessagingBus;

/**
 * Game relay server re-broadcasts any messages it receives to everyone that is connected to it. A
 * game relay can be launched stand-alone, or if a player is hosting a server game they will launch
 * the relay server and then connect to it themselves as a client.
 */
@Slf4j
public class GameRelayServer {
  private final StandaloneWebsocketServer standaloneWebsocketServer;
  private final int port;

  public static URI createLocalhostConnectionUri(final int port) {
    return URI.create("ws://localhost:" + port);
  }

  /**
   * Constructs and starts the game relay server.
   *
   * @param port The local host port that the relay server will open and use to accept connections.
   */
  public GameRelayServer(final int port) {
    this.port = port;
    final WebSocketMessagingBus webSocketMessagingBus = new WebSocketMessagingBus();
    webSocketMessagingBus.addMessageListener(webSocketMessagingBus::broadcastMessage);
    standaloneWebsocketServer = new StandaloneWebsocketServer(webSocketMessagingBus, port);
  }

  public void start() {
    standaloneWebsocketServer.start();
    log.info("Game Relay Server started on port: " + port);
  }

  /**
   * Halts the relay server and frees up the occupied network port. Note, the shutdown is async and
   * can be slow.
   */
  public void stop() {
    standaloneWebsocketServer.shutdown();
  }
}
