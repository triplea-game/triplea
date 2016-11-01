package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

public class EditGameCommentAction extends MenuItem {
  private final InGameLobbyWatcherWrapper m_lobbyWatcher;

  public EditGameCommentAction(final InGameLobbyWatcherWrapper watcher) {
    super("Set Lobby Comment...");
    m_lobbyWatcher = watcher;
    setOnAction(e -> {
      if (!m_lobbyWatcher.isActive()) {
        setDisable(true);
        new Alert(AlertType.WARNING, "Not connected to Lobby").show();
        return;
      }
      TextInputDialog dialog = new TextInputDialog();
      dialog.setTitle("Edit the comments for the game");
      dialog.setContentText("Edit the comments for the game");

      dialog.showAndWait().ifPresent(m_lobbyWatcher::setGameComments);
    });
  }
}
