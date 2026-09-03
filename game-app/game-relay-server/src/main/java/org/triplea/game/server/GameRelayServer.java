package org.triplea.game.server;

import com.google.common.annotations.VisibleForTesting;
import java.net.InetSocketAddress;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.server.relay.GameRelayEnvelope;
import org.triplea.game.server.relay.GameRelayRouter;
import org.triplea.web.socket.GenericWebSocket;
import org.triplea.web.socket.StandaloneWebsocketServer;
import org.triplea.web.socket.WebSocketMessagingBus;

/**
 * A multi-game relay server. It multiplexes many games onto one relay keyed by game-id and routes
 * messages within a game only (see {@link GameRelayRouter}). The first client to join a game is
 * designated that game's host, which holds boot/ban authority.
 *
 * <p>The relay only reads routing metadata; it never parses or deserializes data payloads.
 *
 * <p>It can be launched stand-alone (see {@link RelayServerMain}) for a lobby-managed fleet, or
 * in-process by a player who is hosting a game and then connects to it as a client.
 */
@Slf4j
public class GameRelayServer {
  private final StandaloneWebsocketServer standaloneWebsocketServer;
  private final InetSocketAddress bindAddress;
  private final GameRelayRouter router;

  public static URI createLocalhostConnectionUri(final int port) {
    return URI.create("ws://localhost:" + port);
  }

  /**
   * Constructs a relay bound to the given port on all interfaces.
   *
   * @param port The local port that the relay server will open to accept connections.
   */
  public GameRelayServer(final int port) {
    this(new InetSocketAddress(port));
  }

  /**
   * Constructs a relay bound to a specific address/port (used by the standalone launcher so a fleet
   * host can pin the interface).
   */
  public GameRelayServer(final InetSocketAddress bindAddress) {
    this.bindAddress = bindAddress;
    final WebSocketMessagingBus webSocketMessagingBus = new WebSocketMessagingBus();
    router = new GameRelayRouter(webSocketMessagingBus);
    webSocketMessagingBus.addMessageListener(GameRelayEnvelope.TYPE, router::handleMessage);
    webSocketMessagingBus.addSessionDisconnectListener(
        (bus, session) -> router.onDisconnect(session));
    final GenericWebSocket genericWebSocket =
        new GenericWebSocket(webSocketMessagingBus, router::isIpBanned);
    standaloneWebsocketServer = new StandaloneWebsocketServer(genericWebSocket, bindAddress);
  }

  public void start() {
    standaloneWebsocketServer.start();
    log.info("Game Relay Server started on: " + bindAddress);
  }

  /**
   * Halts the relay server and frees up the occupied network port. Note, the shutdown is async and
   * can be slow.
   */
  public void stop() {
    standaloneWebsocketServer.shutdown();
  }

  @VisibleForTesting
  int memberCount(final String gameId) {
    return router.memberCount(gameId);
  }
}
