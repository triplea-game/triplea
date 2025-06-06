package games.strategy.engine.lobby.client.login;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
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
import org.triplea.domain.data.UserName;
import org.triplea.java.Sha512Hasher;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JCheckBoxBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

final class LoginPanel extends JPanel {
  private static final long serialVersionUID = -1115199161238394717L;

  enum ReturnValue {
    CANCEL,
    LOGON,
    CREATE_ACCOUNT,
    FORGOT_PASSWORD
  }

  private @Nullable JDialog dialog;
  private final JPasswordField password = new JPasswordField();
  private final JTextField username = new JTextField();
  private final JCheckBox rememberPassword =
      new JCheckBoxBuilder("Remember Password").bind(ClientSetting.rememberLoginPassword).build();
  private final JCheckBox anonymousLogin =
      new JCheckBoxBuilder("Login Without An Account")
          .bind(ClientSetting.loginAnonymously)
          .actionListener(selected -> updateComponents())
          .build();
  private final JButton createAccount = new JButton("Create Account");
  private final JButton forgotPassword = new JButton("Forgot Password");
  private ReturnValue returnValue = ReturnValue.CANCEL;
  private final JButton logon = new JButtonBuilder("Login").biggerFont().build();
  private final JButton cancel = new JButton("Cancel");
  private final JLabel passwordLabel = new JLabel("Password:");

  LoginPanel(final LoginMode loginMode) {
    username.setText(String.valueOf(ClientSetting.lobbyLoginName.getValue().orElse(new char[0])));
    password.setText(
        String.valueOf(ClientSetting.lobbySavedPassword.getValue().orElse(new char[0])));

    setLayout(new BorderLayout());

    final JLabel label = new JLabel(new ImageIcon(Util.getBanner("Login")));
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
        username,
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
        passwordLabel,
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
        password,
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
        new JLabel(),
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
        rememberPassword,
        new GridBagConstraints(
            1,
            2,
            1,
            1,
            0.0,
            0.0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(5, 5, 0, 0),
            0,
            0));
    if (loginMode == LoginMode.REGISTRATION_NOT_REQUIRED) {
      main.add(
          new JLabel(),
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
          anonymousLogin,
          new GridBagConstraints(
              1,
              3,
              1,
              1,
              0.0,
              0.0,
              GridBagConstraints.WEST,
              GridBagConstraints.NONE,
              new Insets(5, 5, 0, 0),
              0,
              0));
    } else {
      anonymousLogin.setSelected(false);
    }

    final JPanel buttons = new JPanel();
    add(buttons, BorderLayout.SOUTH);
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    buttons.add(logon);
    buttons.add(createAccount);
    buttons.add(forgotPassword);
    buttons.add(cancel);

    logon.addActionListener(e -> logonPressed());
    createAccount.addActionListener(
        e -> {
          returnValue = ReturnValue.CREATE_ACCOUNT;
          close();
        });
    cancel.addActionListener(e -> close());
    anonymousLogin.addActionListener(e -> updateComponents());
    forgotPassword.addActionListener(
        e -> {
          returnValue = ReturnValue.FORGOT_PASSWORD;
          close();
        });
    SwingKeyBinding.addKeyBinding(this, KeyCode.ENTER, this::logonPressed);
    updateComponents();
  }

  private void close() {
    if (dialog != null) {
      dialog.setVisible(false);
    }
  }

  private void logonPressed() {
    final Optional<String> optionalValidationMessage = UserName.validate(username.getText());

    if (optionalValidationMessage.isPresent()) {
      JOptionPane.showMessageDialog(
          this, optionalValidationMessage.get(), "Invalid Username", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (password.getPassword().length == 0 && !anonymousLogin.isSelected()) {
      JOptionPane.showMessageDialog(
          this, "You must enter a password", "No Password", JOptionPane.ERROR_MESSAGE);
      return;
    } else if (password.getPassword().length < LobbyConstants.PASSWORD_MIN_LENGTH
        && !anonymousLogin.isSelected()) {
      JOptionPane.showMessageDialog(
          this,
          String.format(
              "Passwords must be at least %s characters long", LobbyConstants.PASSWORD_MIN_LENGTH),
          "Invalid Password",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    ClientSetting.lobbyLoginName.setValue(username.getText().toCharArray());
    if (rememberPassword.isSelected()) {
      ClientSetting.lobbySavedPassword.setValueAndFlush(password.getPassword());
    } else {
      ClientSetting.lobbySavedPassword.resetValue();
    }
    returnValue = ReturnValue.LOGON;
    close();
  }

  private void updateComponents() {
    List.of(rememberPassword, passwordLabel, password)
        .forEach(component -> component.setEnabled(!anonymousLogin.isSelected()));
    if (anonymousLogin.isSelected()) {
      password.setText("");
    }
  }

  String getUserName() {
    return username.getText();
  }

  String getPassword() {
    return Sha512Hasher.hashPasswordWithSalt(new String(password.getPassword()));
  }

  ReturnValue show(final Window parent) {
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
}
