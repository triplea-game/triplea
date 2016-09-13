package games.strategy.engine.framework.networkMaintenance;

import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import javafx.scene.AccessibleRole;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

public class SetPasswordAction extends MenuItem {

  public SetPasswordAction(final InGameLobbyWatcherWrapper watcher,
      final ClientLoginValidator validator) {
    super("Set Game Password");
    setOnAction(e -> {
      TextInputDialog dialog = new TextInputDialog();
      dialog.setTitle("Enter Password");
      dialog.setContentText("Enter Password:");
      dialog.getEditor().getStyleClass().add("password-field");
      dialog.getEditor().setAccessibleRole(AccessibleRole.PASSWORD_FIELD);// TODO replace with a real password field to
                                                                          // improve security - copy block etc...
      dialog.showAndWait().ifPresent(password -> {
        final boolean passworded;
        if (password.trim().length() > 0) {
          validator.setGamePassword(password);
          passworded = true;
        } else {
          validator.setGamePassword(null);
          passworded = false;
        }
        if (watcher != null && watcher.isActive()) {
          watcher.setPassworded(passworded);
        }
      });
    });
  }
}
