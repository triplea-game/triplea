package games.strategy.engine.lobby.client.login;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JCheckBoxBuilder;
import org.triplea.swing.SwingComponents;

/** Panel dedicated to changing password after user has logged in with a temporary password. */
public final class ChangePasswordPanel extends JPanel {

  private final String title = "Change Password";
  private @Nullable JDialog dialog;
  private final JPasswordField passwordField = new JPasswordField();
  private final JPasswordField passwordConfirmField = new JPasswordField();
  private final JButton okButton = new JButton("OK");
  private final JCheckBox rememberPassword =
      new JCheckBoxBuilder("Remember Password").bind(ClientSetting.rememberLoginPassword).build();

  private ChangePasswordPanel() {
    layoutComponents();
    setupListeners();
  }

  /**
   * Creates a new instance of the {@code CreateUpdateAccountPanel} class that is used to update the
   * specified lobby account.
   *
   * @return A new {@code CreateUpdateAccountPanel}.
   */
  public static ChangePasswordPanel newChangePasswordPanel() {
    return new ChangePasswordPanel();
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());
    final JLabel label = new JLabel(new ImageIcon(Util.getBanner(title)));
    add(label, BorderLayout.NORTH);

    final JPanel main = new JPanel();
    add(main, BorderLayout.CENTER);
    main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    main.setLayout(new GridBagLayout());
    main.add(
        new JLabel("Password:"),
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 0, 0, 0),
            0,
            0));
    main.add(
        passwordField,
        new GridBagConstraints(
            1,
            0,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(5, 5, 0, 0),
            0,
            0));
    main.add(
        new JLabel("Confirm Password:"),
        new GridBagConstraints(
            0,
            1,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 0, 0, 0),
            0,
            0));
    main.add(
        passwordConfirmField,
        new GridBagConstraints(
            1,
            1,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(5, 5, 0, 0),
            0,
            0));
    main.add(
        rememberPassword,
        new GridBagConstraints(
            0,
            2,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(5, 5, 0, 0),
            0,
            0));

    final JPanel buttons = new JPanel();
    add(buttons, BorderLayout.SOUTH);
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    buttons.add(okButton);
    okButton.setEnabled(false);
  }

  private void setupListeners() {
    okButton.addActionListener(e -> close());

    SwingComponents.addEnterKeyListener(this, this::close);

    DocumentListenerBuilder.attachDocumentListener(
        passwordField, () -> okButton.setEnabled(validatePasswords()));
    DocumentListenerBuilder.attachDocumentListener(
        passwordConfirmField, () -> okButton.setEnabled(validatePasswords()));
  }

  private void close() {
    if (dialog != null) {
      dialog.setVisible(false);
    }
  }

  private boolean validatePasswords() {
    return Arrays.equals(passwordField.getPassword(), passwordConfirmField.getPassword())
        && passwordField.getPassword().length > 4;
  }

  /**
   * Shows this panel in a modal dialog.
   *
   * @param parent The dialog parent window.
   * @return New password entered by user, otherwise null if the window is closed.
   */
  public Optional<String> show(final Window parent) {
    dialog = new JDialog(JOptionPane.getFrameForComponent(parent), title, true);
    dialog.getContentPane().add(this);
    SwingComponents.addEscapeKeyListener(dialog, this::close);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
    dialog.dispose();
    dialog = null;
    if (!validatePasswords()) {
      return Optional.empty();
    }

    final char[] password = passwordField.getPassword();
    if (rememberPassword.isSelected()) {
      ClientSetting.lobbySavedPassword.setValueAndFlush(password);
    } else {
      ClientSetting.lobbySavedPassword.resetValue();
    }
    return Optional.of(String.valueOf(password));
  }
}
