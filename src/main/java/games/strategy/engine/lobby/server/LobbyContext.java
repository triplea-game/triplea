package games.strategy.engine.lobby.server;

import games.strategy.engine.config.LobbyPropertyReader;

/**
 * Container for object creation, useful for managing shared dependencies.
 * Provides somewhat similar functionality as a dependency injection framework.
 */
public final class LobbyContext {
  private static final LobbyContext instance = new LobbyContext();

  private final LobbyPropertyReader lobbyPropertyReader;

  private LobbyContext() {
    lobbyPropertyReader = new LobbyPropertyReader();
  }

  public static LobbyPropertyReader lobbyPropertyReader() {
    return instance.lobbyPropertyReader;
  }

}
