package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;

public class InGameLobbyWatcherWrapper {
  private volatile InGameLobbyWatcher lobbyWatcher = null;

  public void setInGameLobbyWatcher(final InGameLobbyWatcher watcher) {
    lobbyWatcher = watcher;
  }

  public InGameLobbyWatcher getInGameLobbyWatcher() {
    return lobbyWatcher;
  }

  public void shutDown() {
    if (lobbyWatcher != null) {
      lobbyWatcher.shutDown();
    }
  }

  public boolean isActive() {
    return (lobbyWatcher != null) && lobbyWatcher.isActive();
  }

  public String getComments() {
    return (lobbyWatcher == null) ? "" : lobbyWatcher.getComments();
  }

  public void setGameComments(final String comments) {
    if (lobbyWatcher != null) {
      lobbyWatcher.setGameComments(comments);
    }
  }

  public void setGameSelectorModel(final GameSelectorModel model) {
    if (lobbyWatcher != null) {
      lobbyWatcher.setGameSelectorModel(model);
    }
  }

  public void setGameStatus(final GameStatus status, final IGame game) {
    if (lobbyWatcher != null) {
      lobbyWatcher.setGameStatus(status, game);
    }
  }

  public void setPassworded(final boolean passworded) {
    if (lobbyWatcher != null) {
      lobbyWatcher.setPassworded(passworded);
    }
  }
}
