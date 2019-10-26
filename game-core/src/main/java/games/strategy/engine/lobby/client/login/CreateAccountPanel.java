package games.strategy.engine.lobby.client.login;

import games.strategy.engine.lobby.PlayerNameValidation;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
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
import javax.swing.JTextField;
import org.triplea.domain.data.PlayerEmailValidation;
import org.triplea.swing.JCheckBoxBuilder;
import org.triplea.swing.SwingComponents;

/** The panel used to create a new lobby account or update an existing lobby account. */
public final class CreateAccountPanel extends JPanel {
  private static final long serialVersionUID = 2285956517232671122L;

  /** Indicates how the user dismissed the dialog displaying the panel. */
  public enum ReturnValue {
    CANCEL,
    OK
  }

  private static final String TITLE = "Create Account";
  private @Nullable JDialog dialog;
  private final JTextField usernameField = new JTextField();
  private final JTextField emailField = new JTextField();
  private final JPasswordField passwordField = new JPasswordField();
  private final JPasswordField passwordConfirmField = new JPasswordField();
  private ReturnValue returnValue = ReturnValue.CANCEL;

  CreateAccountPanel() {
    setLayout(new BorderLayout());
    final JLabel label = new JLabel(new ImageIcon(Util.getBanner(TITLE)));
    add(label, BorderLayout.NORTH);

    final JPanel main = new JPanel();
    add(main, BorderLayout.CENTER);
    main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    main.setLayout(new GridBagLayout());
    main.add(
        new JLabel("Username:"),
        new GridBagConstraints(
            0,
            0,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    main.add(
        usernameField,
        new GridBagConstraints(
            1,
            0,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 5, 0, 0),
            0,
            0));
    main.add(
        new JLabel("Password:"),
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
        passwordField,
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
        new JLabel("Confirm Password:"),
        new GridBagConstraints(
            0,
            2,
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
            2,
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
        new JLabel("Email:"),
        new GridBagConstraints(
            0,
            3,
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
        emailField,
        new GridBagConstraints(
            1,
            3,
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
        new JLabel(),
        new GridBagConstraints(
            0,
            4,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 0, 0, 0),
            0,
            0));
    final JCheckBox rememberPasswordCheckbox =
        new JCheckBoxBuilder("Remember Password").bind(ClientSetting.rememberLoginPassword).build();
    main.add(
        rememberPasswordCheckbox,
        new GridBagConstraints(
            1,
            4,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 0, 0),
            0,
            0));

    final JPanel buttons = new JPanel();
    add(buttons, BorderLayout.SOUTH);
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    final JButton okButton = new JButton("OK");
    buttons.add(okButton);
    final JButton cancelButton = new JButton("Cancel");
    buttons.add(cancelButton);

    cancelButton.addActionListener(e -> close());
    okButton.addActionListener(e -> okPressed());

    SwingComponents.addEnterKeyListener(this, this::okPressed);
  }

  private void close() {
    if (dialog != null) {
      dialog.setVisible(false);
    }
  }

  private void okPressed() {
    if (!Arrays.equals(passwordField.getPassword(), passwordConfirmField.getPassword())) {
      JOptionPane.showMessageDialog(
          this, "The passwords do not match", "Passwords Do Not Match", JOptionPane.ERROR_MESSAGE);
      passwordField.setText("");
      passwordConfirmField.setText("");
      return;
    } else if (!PlayerEmailValidation.isValid(emailField.getText())) {
      JOptionPane.showMessageDialog(
          this, "You must enter a valid email", "No Email", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (!PlayerNameValidation.isValid(usernameField.getText())) {
      JOptionPane.showMessageDialog(
          this,
          PlayerNameValidation.validate(usernameField.getText()),
          "Invalid name",
          JOptionPane.ERROR_MESSAGE);
      return;
    } else if (emailField.getText().isEmpty()) {
      JOptionPane.showMessageDialog(
          this, "You must enter an email", "No Email", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (passwordField.getPassword().length == 0) {
      JOptionPane.showMessageDialog(
          this, "You must enter a password", "No Password", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (passwordField.getPassword().length < 3) {
      JOptionPane.showMessageDialog(
          this,
          "Passwords must be at least three characters long",
          "Invalid Password",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    returnValue = ReturnValue.OK;
    close();
  }

  /**
   * Shows this panel in a modal dialog.
   *
   * @param parent The dialog parent window.
   * @return {@link ReturnValue#OK} if the user confirmed that the lobby account should be
   *     created/updated; otherwise {@link ReturnValue#CANCEL}.
   */
  public ReturnValue show(final Window parent) {
    dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "", true);
    dialog.getContentPane().add(this);
    SwingComponents.addEscapeKeyListener(dialog, this::close);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
    dialog.dispose();
    dialog = null;
    return returnValue;
  }

  public String getPassword() {
    return new String(passwordField.getPassword());
  }

  String getEmail() {
    return emailField.getText();
  }

  String getUserName() {
    return usernameField.getText();
  }
}
