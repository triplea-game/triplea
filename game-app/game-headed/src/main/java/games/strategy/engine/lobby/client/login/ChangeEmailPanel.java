package games.strategy.engine.lobby.client.login;

import games.strategy.ui.Util;
import java.awt.Window;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.triplea.domain.data.LobbyConstants;
import org.triplea.domain.data.PlayerEmailValidation;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.jpanel.FlowLayoutBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/** Panel dedicated to changing user email. */
public final class ChangeEmailPanel {
  private static final String TITLE = "Change Email";

  private JPanel createPanel(
      final JDialog dialog, final String userExistingEmail, final Consumer<String> submitAction) {
    final JTextField emailField =
        JTextFieldBuilder.builder()
            .text(userExistingEmail)
            .maxLength(LobbyConstants.EMAIL_MAX_LENGTH)
            .build();
    final JButton okButton = new JButton("OK");
    okButton.setEnabled(false);

    final JPanel panel =
        new JPanelBuilder()
            .borderLayout()
            .addNorth(new JLabel(new ImageIcon(Util.getBanner(TITLE))))
            .addCenter(
                new JPanelBuilder()
                    .border(BorderFactory.createEmptyBorder(10, 10, 10, 10))
                    .gridBagLayout()
                    .add(
                        new JLabel("Email:"),
                        new GridBagConstraintsBuilder(0, 0).insets(5, 0, 0, 0).build())
                    .add(
                        emailField,
                        new GridBagConstraintsBuilder(1, 0)
                            .fill(GridBagConstraintsFill.HORIZONTAL)
                            .weightX(1.0)
                            .insets(5, 5, 0, 0)
                            .build())
                    .build())
            .addSouth(
                new JPanelBuilder()
                    .border(BorderFactory.createEmptyBorder(10, 5, 10, 5))
                    .flowLayout()
                    .flowDirection(FlowLayoutBuilder.Direction.RIGHT)
                    .hgap(5)
                    .vgap(0)
                    .add(okButton)
                    .add(
                        new JButtonBuilder()
                            .title("Cancel")
                            .actionListener(dialog::dispose)
                            .build())
                    .build())
            .build();

    okButton.addActionListener(
        e -> {
          dialog.dispose();
          submitAction.accept(emailField.getText());
        });
    SwingKeyBinding.addKeyBinding(
        panel,
        KeyCode.ENTER,
        () -> {
          if (okButton.isEnabled()) {
            dialog.dispose();
            submitAction.accept(emailField.getText());
          }
        });
    SwingKeyBinding.addKeyBinding(panel, KeyCode.ESCAPE, dialog::dispose);

    new DocumentListenerBuilder(
            () ->
                okButton.setEnabled(
                    !emailField.getText().equals(userExistingEmail)
                        && PlayerEmailValidation.isValid(emailField.getText())))
        .attachTo(emailField);

    return panel;
  }

  /** Shows a dialog to user to update password. */
  public static void promptUserForNewEmail(
      final Window parent, final PlayerToLobbyConnection playerToLobbyConnection) {

    final String existingUserEmail = playerToLobbyConnection.fetchEmail();

    final JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "", true);

    final Consumer<String> submitAction =
        newEmail -> {
          playerToLobbyConnection.changeEmail(newEmail);
          DialogBuilder.builder()
              .parent(parent)
              .title("Success")
              .infoMessage("Email updated to: " + newEmail)
              .showDialog();
        };

    final JPanel panel =
        new ChangeEmailPanel().createPanel(dialog, existingUserEmail, submitAction);
    dialog.getContentPane().add(panel);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
    dialog.dispose();
  }
}
