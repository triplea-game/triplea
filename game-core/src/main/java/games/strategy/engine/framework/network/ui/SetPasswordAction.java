package games.strategy.engine.framework.network.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;

/**
 * An action for setting the network game password.
 */
public class SetPasswordAction extends AbstractAction {
  private static final long serialVersionUID = -7767288210554177480L;
  private final ClientLoginValidator validator;
  private final Component parent;
  private final InGameLobbyWatcherWrapper lobbyWatcher;

  public SetPasswordAction(final Component parent, final InGameLobbyWatcherWrapper watcher,
      final ClientLoginValidator validator) {
    super("Set Game Password");
    this.validator = validator;
    this.parent = parent;
    this.lobbyWatcher = watcher;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final JLabel label = new JLabel("Enter Password, (Leave blank for no password).");
    final JPasswordField passwordField = new JPasswordField();
    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(label, BorderLayout.NORTH);
    panel.add(passwordField, BorderLayout.CENTER);
    final int selectedOption = JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(parent), panel,
        "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    final String password = new String(passwordField.getPassword());
    final boolean passworded;
    if (password.trim().length() > 0) {
      validator.setGamePassword(password);
      passworded = true;
    } else {
      validator.setGamePassword(null);
      passworded = false;
    }
    if ((lobbyWatcher != null) && lobbyWatcher.isActive()) {
      lobbyWatcher.setPassworded(passworded);
    }
  }
}
