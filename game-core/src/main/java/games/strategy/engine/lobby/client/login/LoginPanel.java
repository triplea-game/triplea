package games.strategy.engine.lobby.client.login;

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
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.ui.SwingComponents;
import games.strategy.ui.Util;

final class LoginPanel extends JPanel {
  private static final long serialVersionUID = -1115199161238394717L;

  enum ReturnValue {
    CANCEL, LOGON, CREATE_ACCOUNT
  }

  private @Nullable JDialog dialog;
  private final JPasswordField password = new JPasswordField();
  private final JTextField userName = new JTextField();
  private final JCheckBox credentialsSaved = new JCheckBox("Remember me");
  private final JCheckBox anonymousLogin = new JCheckBox("Login anonymously");
  private final JButton createAccount = new JButton("Create Account...");
  private ReturnValue returnValue = ReturnValue.CANCEL;
  private final JButton logon = new JButton("Login");
  private final JButton cancel = new JButton("Cancel");

  LoginPanel(final LobbyLoginPreferences lobbyLoginPreferences) {
    layoutComponents();
    setupListeners();
    initializeComponents(lobbyLoginPreferences);
    updateComponents();
  }

  private void initializeComponents(final LobbyLoginPreferences lobbyLoginPreferences) {
    userName.setText(lobbyLoginPreferences.userName);
    password.setText(lobbyLoginPreferences.password);
    credentialsSaved.setSelected(lobbyLoginPreferences.credentialsSaved);
    anonymousLogin.setSelected(lobbyLoginPreferences.anonymousLogin);
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());

    final JLabel label = new JLabel(new ImageIcon(Util.getBanner("Login")));
    add(label, BorderLayout.NORTH);

    final JPanel main = new JPanel();
    add(main, BorderLayout.CENTER);
    main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    main.setLayout(new GridBagLayout());
    main.add(new JLabel("Username:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    main.add(userName, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
    main.add(new JLabel("Password:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    main.add(password, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 0), 0, 0));
    main.add(new JLabel(), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    main.add(credentialsSaved, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
    main.add(new JLabel(), new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
    main.add(anonymousLogin, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));

    final JPanel buttons = new JPanel();
    add(buttons, BorderLayout.SOUTH);
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));
    buttons.add(logon);
    buttons.add(createAccount);
    buttons.add(cancel);
  }

  private void setupListeners() {
    logon.addActionListener(e -> logonPressed());
    createAccount.addActionListener(e -> {
      returnValue = ReturnValue.CREATE_ACCOUNT;
      close();
    });
    cancel.addActionListener(e -> close());
    anonymousLogin.addActionListener(e -> updateComponents());

    SwingComponents.addEnterKeyListener(this, this::logonPressed);
  }

  private void close() {
    if (dialog != null) {
      dialog.setVisible(false);
    }
  }

  private void logonPressed() {
    if (!DBUser.isValidUserName(userName.getText())) {
      JOptionPane.showMessageDialog(this, DBUser.getUserNameValidationErrorMessage(userName.getText()),
          "Invalid Username", JOptionPane.ERROR_MESSAGE);
      return;
    } else if ((password.getPassword().length == 0) && !anonymousLogin.isSelected()) {
      JOptionPane.showMessageDialog(this, "You must enter a password", "No Password", JOptionPane.ERROR_MESSAGE);
      return;
    } else if ((password.getPassword().length < 3) && !anonymousLogin.isSelected()) {
      JOptionPane.showMessageDialog(this, "Passwords must be at least three characters long", "Invalid Password",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    returnValue = ReturnValue.LOGON;
    close();
  }

  private void updateComponents() {
    password.setEnabled(!anonymousLogin.isSelected());
  }

  boolean isAnonymousLogin() {
    return anonymousLogin.isSelected();
  }

  String getUserName() {
    return userName.getText();
  }

  String getPassword() {
    return new String(password.getPassword());
  }

  LobbyLoginPreferences getLobbyLoginPreferences() {
    return new LobbyLoginPreferences(getUserName(), getPassword(), credentialsSaved.isSelected(), isAnonymousLogin());
  }

  ReturnValue show(final Window parent) {
    dialog = new JDialog(JOptionPane.getFrameForComponent(parent), "Login", true);
    dialog.getContentPane().add(this);
    SwingComponents.addEscapeKeyListener(dialog, this::close);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
    dialog.dispose();
    dialog = null;
    return returnValue;
  }
}
