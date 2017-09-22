package games.strategy.engine.lobby.client.login;

import static com.google.common.base.Preconditions.checkNotNull;

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

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.ui.Util;

/**
 * The panel used to create a new lobby account or update an existing lobby account.
 */
public final class CreateUpdateAccountPanel extends JPanel {
  private static final long serialVersionUID = 2285956517232671122L;

  /**
   * Indicates how the user dismissed the dialog displaying the panel.
   */
  public enum ReturnValue {
    CANCEL, OK
  }

  private final String title;
  private @Nullable JDialog dialog;
  private final JTextField userNameField = new JTextField();
  private final JTextField emailField = new JTextField();
  private final JPasswordField passwordField = new JPasswordField();
  private final JPasswordField passwordConfirmField = new JPasswordField();
  private final JCheckBox credentialsSavedCheckBox = new JCheckBox("Remember me");
  private final JButton okButton = new JButton("OK");
  private final JButton cancelButton = new JButton("Cancel");
  private ReturnValue returnValue = ReturnValue.CANCEL;

  /**
   * Creates a new instance of the {@code CreateUpdateAccountPanel} class that is used to update the specified lobby
   * account.
   *
   * @param user The lobby account to update.
   * @param lobbyLoginPreferences The user's lobby login preferences.
   *
   * @return A new {@code CreateUpdateAccountPanel}.
   */
  public static CreateUpdateAccountPanel newUpdatePanel(
      final DBUser user,
      final LobbyLoginPreferences lobbyLoginPreferences) {
    checkNotNull(user);
    checkNotNull(lobbyLoginPreferences);

    final CreateUpdateAccountPanel panel = new CreateUpdateAccountPanel(false);
    panel.userNameField.setText(user.getName());
    panel.userNameField.setEnabled(false);
    panel.emailField.setText(user.getEmail());
    panel.credentialsSavedCheckBox.setSelected(lobbyLoginPreferences.credentialsSaved);
    return panel;
  }

  /**
   * Creates a new instance of the {@code CreateUpdateAccountPanel} class that is used to create a new lobby account.
   *
   * @return A new {@code CreateUpdateAccountPanel}.
   */
  public static CreateUpdateAccountPanel newCreatePanel() {
    return new CreateUpdateAccountPanel(true);
  }

  private CreateUpdateAccountPanel(final boolean create) {
    title = create ? "Create Account" : "Update Account";

    layoutComponents();
    setupListeners();
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());
    final JLabel label = new JLabel(new ImageIcon(Util.getBanner(title)));
    add(label, BorderLayout.NORTH);

    final JPanel main = new JPanel();
    add(main, BorderLayout.CENTER);
    main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    main.setLayout(new GridBagLayout());
    main.add(new JLabel("Username:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    main.add(userNameField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    main.add(new JLabel("Password:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    main.add(passwordField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
    main.add(new JLabel("Confirm Password:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    main.add(passwordConfirmField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
    main.add(new JLabel("Email:"), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    main.add(emailField, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
    main.add(new JLabel(), new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    main.add(credentialsSavedCheckBox, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));

    final JPanel buttons = new JPanel();
    add(buttons, BorderLayout.SOUTH);
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    buttons.add(okButton);
    buttons.add(cancelButton);
  }

  private void setupListeners() {
    cancelButton.addActionListener(e -> dialog.setVisible(false));
    okButton.addActionListener(e -> okPressed());
  }

  private void okPressed() {
    if (!Arrays.equals(passwordField.getPassword(), passwordConfirmField.getPassword())) {
      JOptionPane.showMessageDialog(this, "The passwords do not match", "Passwords Do Not Match",
          JOptionPane.ERROR_MESSAGE);
      passwordField.setText("");
      passwordConfirmField.setText("");
      return;
    } else if (!games.strategy.util.Util.isMailValid(emailField.getText())) {
      JOptionPane.showMessageDialog(this, "You must enter a valid email", "No Email", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (!DBUser.isValidUserName(userNameField.getText())) {
      JOptionPane.showMessageDialog(
          this,
          DBUser.getUserNameValidationErrorMessage(userNameField.getText()),
          "Invalid name",
          JOptionPane.ERROR_MESSAGE);
      return;
    } else if (passwordField.getPassword().length == 0) {
      JOptionPane.showMessageDialog(this, "You must enter a password", "No Password", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (passwordField.getPassword().length < 3) {
      JOptionPane.showMessageDialog(this, "Passwords must be at least three characters long", "Invalid Password",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    returnValue = ReturnValue.OK;
    dialog.setVisible(false);
  }

  /**
   * Shows this panel in a modal dialog.
   *
   * @param parent The dialog parent window.
   *
   * @return {@link ReturnValue#OK} if the user confirmed that the lobby account should be created/updated; otherwise
   *         {@link ReturnValue#CANCEL}.
   */
  public ReturnValue show(final Window parent) {
    dialog = new JDialog(JOptionPane.getFrameForComponent(parent), title, true);
    dialog.getContentPane().add(this);
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

  public String getEmail() {
    return emailField.getText();
  }

  public String getUserName() {
    return userNameField.getText();
  }

  public LobbyLoginPreferences getLobbyLoginPreferences() {
    return new LobbyLoginPreferences(getUserName(), getPassword(), credentialsSavedCheckBox.isSelected(), false);
  }
}
