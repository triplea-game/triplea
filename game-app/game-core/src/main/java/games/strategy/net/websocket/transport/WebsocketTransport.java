package games.strategy.net.websocket.transport;

/**
 * Shared constants for the round-1 websocket relay transport (gated by {@code
 * useWebsocketTransport}).
 */
public final class WebsocketTransport {

  /**
   * Round-1 shared game-id. One in-process relay runs per host machine and serves exactly one game,
   * so a single constant is enough for the host and every client to agree on the same relay room.
   * Lobby-driven, per-game ids that let one relay multiplex many games are round 2.
   */
  public static final String ROUND1_GAME_ID = "triplea-round1-local-game";

  private WebsocketTransport() {}
}
