package games.strategy.engine.lobby.server;

/**
 * Container for object creation, useful for managing shared dependencies.
 * Provides somewhat similar functionality as a dependency injection framework.
 */
public final class LobbyContext {
  private static final LobbyContext instance = new LobbyContext();

  private LobbyContext() {
  }
}
