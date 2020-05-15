package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import java.util.Optional;
import org.triplea.lobby.common.GameDescription.GameStatus;

/**
 * A proxy for an {@link InGameLobbyWatcher} to accommodate dynamically changing the underlying
 * lobby watcher as games are started and stopped on the host.
 */
public class InGameLobbyWatcherWrapper {
  private volatile InGameLobbyWatcher lobbyWatcher = null;

  public Optional<String> getGameId() {
    return Optional.of(lobbyWatcher).map(InGameLobbyWatcher::getGameId);
  }

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
    return lobbyWatcher != null && lobbyWatcher.isActive();
  }

  public String getComments() {
    return lobbyWatcher == null ? "" : lobbyWatcher.getComments();
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

  public void executeConnectivityCheck() {
    lobbyWatcher.executeConnectivityCheck();
  }
}
