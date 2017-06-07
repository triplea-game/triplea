package games.strategy.engine.lobby.client.login;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import games.strategy.engine.lobby.server.userDB.DbUser;
import games.strategy.ui.Util;

public class CreateUpdateAccountPanel extends JPanel {
  private static final long serialVersionUID = 2285956517232671122L;

  public static enum ReturnValue {
    CANCEL, OK
  }

  private JDialog dialog;
  private JTextField userNameField;
  private JTextField emailField;
  private JPasswordField passwordField;
  private JPasswordField passwordConfirmField;
  private JButton okButton;
  private JButton cancelButton;
  private ReturnValue returnValue;

  public static CreateUpdateAccountPanel newUpdatePanel(final DbUser user) {
    final CreateUpdateAccountPanel panel = new CreateUpdateAccountPanel(false);
    panel.userNameField.setText(user.getName());
    panel.userNameField.setEditable(false);
    panel.emailField.setText(user.getEmail());
    return panel;
  }

  public static CreateUpdateAccountPanel newCreatePanel() {
    final CreateUpdateAccountPanel panel = new CreateUpdateAccountPanel(true);
    return panel;
  }

  private CreateUpdateAccountPanel(final boolean create) {
    createComponents();
    layoutComponents(create);
    setupListeners();
    setWidgetActivation();
  }

  private void createComponents() {
    userNameField = new JTextField();
    emailField = new JTextField();
    passwordField = new JPasswordField();
    passwordConfirmField = new JPasswordField();
    cancelButton = new JButton("Cancel");
    okButton = new JButton("OK");
  }

  private void layoutComponents(final boolean create) {
    final JLabel label = new JLabel(new ImageIcon(Util.getBanner(create ? "Create Account" : "Update Account")));
    setLayout(new BorderLayout());
    add(label, BorderLayout.NORTH);
    final JPanel main = new JPanel();
    add(main, BorderLayout.CENTER);
    main.setLayout(new GridBagLayout());
    main.add(new JLabel("Username:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));
    main.add(userNameField, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(10, 5, 0, 40), 0, 0));
    main.add(new JLabel("Password:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
    main.add(passwordField, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(5, 5, 0, 40), 0, 0));
    main.add(new JLabel("Re-type Password:"), new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
    main.add(passwordConfirmField, new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.BOTH, new Insets(5, 5, 0, 40), 0, 0));
    main.add(new JLabel("Email:"), new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 20, 15, 0), 0, 0));
    main.add(emailField, new GridBagConstraints(1, 3, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(5, 5, 15, 40), 0, 0));
    final JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(okButton);
    buttons.add(cancelButton);
    add(buttons, BorderLayout.SOUTH);
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
    }
    if (!games.strategy.util.Util.isMailValid(emailField.getText())) {
      JOptionPane.showMessageDialog(this, "You must enter a valid email", "No email", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (!DbUser.isValidUserName(userNameField.getText())) {
      JOptionPane.showMessageDialog(
          this,
          DbUser.getUserNameValidationErrorMessage(userNameField.getText()),
          "Invalid name",
          JOptionPane.ERROR_MESSAGE);
      return;
    } else if (passwordField.getPassword().length == 0) {
      JOptionPane.showMessageDialog(this, "You must enter a password", "No Password", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (passwordField.getPassword().length < 3) {
      JOptionPane.showMessageDialog(this, "Passwords must be at least three characters long", "Invalid password",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    returnValue = ReturnValue.OK;
    dialog.setVisible(false);
  }

  private void setWidgetActivation() {}

  public ReturnValue show(final Window parent) {
    dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "Login", true);
    dialog.getContentPane().add(this);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
    dialog.dispose();
    dialog = null;
    if (returnValue == null) {
      return ReturnValue.CANCEL;
    }
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
}
