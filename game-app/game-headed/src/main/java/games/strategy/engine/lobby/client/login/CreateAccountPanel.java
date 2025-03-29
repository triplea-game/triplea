package games.strategy.engine.lobby.client.login;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
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
import javax.swing.JTextField;
import org.triplea.domain.data.LobbyConstants;
import org.triplea.domain.data.PlayerEmailValidation;
import org.triplea.domain.data.UserName;
import org.triplea.java.Sha512Hasher;
import org.triplea.swing.JCheckBoxBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.KeyTypeValidator;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/** The panel used to create a new lobby account or update an existing lobby account. */
public final class CreateAccountPanel extends JPanel {
  private static final long serialVersionUID = 2285956517232671122L;
  public static final int ACCOUNT_PASSWORD_MIN_LENGTH = 5;

  /** Indicates how the user dismissed the dialog displaying the panel. */
  public enum ReturnValue {
    CANCEL,
    OK
  }

  private static final String TITLE = "Create Account";
  private @Nullable JDialog dialog;
  private final JTextField usernameField =
      JTextFieldBuilder.builder().maxLength(LobbyConstants.USERNAME_MAX_LENGTH).build();
  private final JTextField emailField =
      JTextFieldBuilder.builder().maxLength(LobbyConstants.EMAIL_MAX_LENGTH).build();
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

    final JLabel validationLabel = new JLabel(" ");
    validationLabel.setForeground(Color.RED);

    main.add(
        validationLabel,
        new GridBagConstraints(
            0,
            5,
            2,
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
    okButton.setEnabled(false);
    buttons.add(okButton);
    final JButton cancelButton = new JButton("Cancel");
    buttons.add(cancelButton);

    cancelButton.addActionListener(e -> close());
    okButton.addActionListener(e -> okPressed());

    SwingKeyBinding.addKeyBinding(
        this,
        KeyCode.ENTER,
        () -> {
          if (okButton.isEnabled()) {
            okPressed();
          }
        });

    List.of(passwordField, passwordConfirmField, emailField)
        .forEach(
            inputField ->
                new KeyTypeValidator()
                    .attachKeyTypeValidator(
                        inputField,
                        textInput -> validationMessage().isEmpty(),
                        valid -> {
                          if (valid) {
                            validationLabel.setText(" ");
                            okButton.setEnabled(true);
                            okButton.setToolTipText("Click to create an account and login");
                          } else {
                            okButton.setEnabled(false);

                            final String message = validationMessage().orElse("");
                            validationLabel.setText(message);
                            okButton.setToolTipText(message);
                          }
                        }));
  }

  private void okPressed() {
    returnValue = ReturnValue.OK;
    Optional.ofNullable(dialog).ifPresent(d -> d.setVisible(false));
  }

  private void close() {
    if (dialog != null) {
      dialog.setVisible(false);
    }
  }

  private Optional<String> validationMessage() {
    if (!Arrays.equals(passwordField.getPassword(), passwordConfirmField.getPassword())) {
      return Optional.of("Passwords must match");
    } else if (!PlayerEmailValidation.isValid(emailField.getText())) {
      return Optional.of("Email must be valid");
    } else if (!UserName.isValid(usernameField.getText())) {
      return Optional.ofNullable(UserName.validate(usernameField.getText()));
    } else if (emailField.getText().isEmpty()) {
      return Optional.of("You must enter an email");
    } else if (passwordField.getPassword().length == 0) {
      return Optional.of("You must enter a password");
    } else if (passwordField.getPassword().length < ACCOUNT_PASSWORD_MIN_LENGTH) {
      return Optional.of(
          String.format(
              "Passwords must be at least %s characters long", LobbyConstants.PASSWORD_MIN_LENGTH));
    }
    return Optional.empty();
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
    SwingKeyBinding.addKeyBinding(dialog, KeyCode.ESCAPE, this::close);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
    dialog.dispose();
    dialog = null;
    return returnValue;
  }

  public String getPassword() {
    return Sha512Hasher.hashPasswordWithSalt(new String(passwordField.getPassword()));
  }

  String getEmail() {
    return emailField.getText();
  }

  String getUsername() {
    return usernameField.getText();
  }
}
