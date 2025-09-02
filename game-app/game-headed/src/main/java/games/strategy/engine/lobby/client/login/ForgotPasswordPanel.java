package games.strategy.engine.lobby.client.login;

import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.annotation.Nullable;
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
import org.triplea.domain.data.UserName;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/**
 * Panel used to request a new temporary password. The panel displays a single text field for
 * username.
 */
final class ForgotPasswordPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  /** Indicates how the user dismissed the dialog displaying the panel. */
  public enum ReturnValue {
    CANCEL,
    OK
  }

  private static final String TITLE = "Forgot Password";
  private @Nullable JDialog dialog;
  private final JTextField userNameField = new JTextField();
  private final JTextField emailField =
      JTextFieldBuilder.builder().maxLength(LobbyConstants.EMAIL_MAX_LENGTH).build();
  private final JButton okButton = new JButton("OK");
  private final JButton cancelButton = new JButton("Cancel");
  private ReturnValue returnValue = ReturnValue.CANCEL;

  private ForgotPasswordPanel() {
    layoutComponents();
    setupListeners();
  }

  /**
   * Creates a new instance of the {@code CreateUpdateAccountPanel} class used to request a
   * temporary password.
   *
   * @return A new {@code CreateUpdateAccountPanel}.
   */
  static ForgotPasswordPanel newForgotPasswordPanel() {
    return new ForgotPasswordPanel();
  }

  private void layoutComponents() {
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
        userNameField,
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
        new JLabel("Email: "),
        new GridBagConstraints(
            0,
            1,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 5, 0, 0),
            0,
            0));
    main.add(
        emailField,
        new GridBagConstraints(
            1,
            1,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(0, 5, 0, 0),
            0,
            0));
    final JPanel buttons = new JPanel();
    add(buttons, BorderLayout.SOUTH);
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    buttons.add(okButton);
    buttons.add(cancelButton);
  }

  private void setupListeners() {
    cancelButton.addActionListener(e -> close());
    okButton.addActionListener(e -> okPressed());

    SwingKeyBinding.addKeyBinding(this, KeyCode.ENTER, this::okPressed);
  }

  private void close() {
    if (dialog != null) {
      dialog.setVisible(false);
    }
  }

  private void okPressed() {
    if (!UserName.isValid(userNameField.getText())) {
      JOptionPane.showMessageDialog(
          this,
          UserName.validate(userNameField.getText())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          String.format(
                              "UserName.isValid return false, but validate return is empty for %s",
                              userNameField.getText()))),
          "Invalid name",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (!PlayerEmailValidation.isValid(emailField.getText())) {
      JOptionPane.showMessageDialog(
          this,
          PlayerEmailValidation.validate(emailField.getText()),
          "Invalid email",
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
   * @return {@link ReturnValue#OK} if the user clicks 'okay' button, otherwise {@link
   *     ReturnValue#CANCEL}.
   */
  ReturnValue show(final Window parent) {
    dialog = new JDialog(JOptionPane.getFrameForComponent(parent), TITLE, true);
    dialog.getContentPane().add(this);
    SwingKeyBinding.addKeyBinding(dialog, KeyCode.ESCAPE, this::close);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
    dialog.dispose();
    dialog = null;
    return returnValue;
  }

  String getUserName() {
    return userNameField.getText();
  }

  String getEmail() {
    return emailField.getText();
  }
}
