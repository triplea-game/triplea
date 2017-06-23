package games.strategy.engine.lobby.client.login;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.ui.SwingAction;
import games.strategy.ui.Util;

class LoginPanel extends JPanel {
  private static final long serialVersionUID = -1115199161238394717L;
  private static final Logger logger = Logger.getLogger(LoginPanel.class.getName());

  enum ReturnValue {
    CANCEL, LOGON, CREATE_ACCOUNT
  }

  static final String LAST_LOGIN_NAME_PREF = "LAST_LOGIN_NAME_PREF";
  static final String ANONYMOUS_LOGIN_PREF = "ANONYMOUS_LOGIN_PREF";
  private JDialog dialog;
  private JPasswordField password;
  private JTextField userName;
  private JCheckBox anonymous;
  private JButton createAccount;
  private ReturnValue returnValue;
  private JButton logon;
  private JButton cancel;

  LoginPanel() {
    createComponents();
    layoutComponents();
    setupListeners();
    readDefaults();
    setWidgetActivation();
  }

  private void readDefaults() {
    final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    final String name = prefs.get(LAST_LOGIN_NAME_PREF, System.getProperty("user.name"));
    final boolean anonymous = prefs.getBoolean(ANONYMOUS_LOGIN_PREF, true);
    this.anonymous.setSelected(anonymous);
    userName.setText(name);
    SwingUtilities.invokeLater(() -> {
      if (!this.anonymous.isSelected()) {
        password.requestFocusInWindow();
      } else {
        userName.requestFocusInWindow();
      }
    });
  }

  private void createComponents() {
    password = new JPasswordField();
    userName = new JTextField();
    anonymous = new JCheckBox("Login Anonymously?");
    createAccount = new JButton("Create Account...");
    logon = new JButton("Login");
    cancel = new JButton("Cancel");
  }

  private void layoutComponents() {
    final JLabel label = new JLabel(new ImageIcon(Util.getBanner("Login")));
    setLayout(new BorderLayout());
    add(label, BorderLayout.NORTH);
    final JPanel main = new JPanel();
    add(main, BorderLayout.CENTER);
    main.setLayout(new GridBagLayout());
    main.add(new JLabel("Username:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));
    main.add(userName, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(10, 5, 0, 40), 0, 0));
    main.add(new JLabel("Password:"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 20, 0, 0), 0, 0));
    main.add(password, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(5, 5, 0, 40), 0, 0));
    main.add(anonymous, new GridBagConstraints(0, 2, 2, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(5, 20, 0, 0), 0, 0));
    main.add(createAccount, new GridBagConstraints(0, 3, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
        new Insets(5, 20, 0, 0), 0, 0));
    final JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(logon);
    buttons.add(cancel);
    add(buttons, BorderLayout.SOUTH);
  }

  private void setupListeners() {
    logon.addActionListener(e -> logonPressed());
    createAccount.addActionListener(e -> {
      returnValue = ReturnValue.CREATE_ACCOUNT;
      dialog.setVisible(false);
    });
    cancel.addActionListener(e -> dialog.setVisible(false));
    anonymous.addActionListener(e -> setWidgetActivation());
    // close when hitting the escape key
    final Action enterAction = SwingAction.of(e -> logonPressed());
    final String key = "logon.through.enter.key";
    getActionMap().put(key, enterAction);
    getActionMap().put(key, enterAction);
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), key);
  }

  private void logonPressed() {
    final String userName = this.userName.getText();
    final boolean anonymous = this.anonymous.isSelected();
    if (!DBUser.isValidUserName(userName)) {
      JOptionPane.showMessageDialog(this, DBUser.getUserNameValidationErrorMessage(userName), "Invalid Username",
          JOptionPane.ERROR_MESSAGE);
      return;
    } else if (password.getPassword().length == 0 && !anonymous) {
      JOptionPane.showMessageDialog(LoginPanel.this, "You must enter a password", "No Password",
          JOptionPane.ERROR_MESSAGE);
      return;
    } else if (password.getPassword().length < 3 && !anonymous) {
      JOptionPane.showMessageDialog(LoginPanel.this, "Passwords must be at least three characters long",
          "Invalid password", JOptionPane.ERROR_MESSAGE);
      return;
    }
    returnValue = ReturnValue.LOGON;
    dialog.setVisible(false);
  }

  static void storePrefs(final String userName, final boolean anonymous) {
    final Preferences prefs = Preferences.userNodeForPackage(LoginPanel.class);
    prefs.put(LAST_LOGIN_NAME_PREF, userName);
    prefs.putBoolean(ANONYMOUS_LOGIN_PREF, anonymous);
    try {
      prefs.flush();
    } catch (final BackingStoreException e) {
      // not a big deal
      logger.warning(e.getMessage());
    }
  }

  private void setWidgetActivation() {
    if (!anonymous.isSelected()) {
      password.setEnabled(true);
      password.setBackground(userName.getBackground());
    } else {
      password.setEnabled(false);
      password.setBackground(this.getBackground());
    }
  }

  boolean isAnonymous() {
    return anonymous.isSelected();
  }

  String getUserName() {
    return userName.getText();
  }

  String getPassword() {
    return new String(password.getPassword());
  }

  ReturnValue show(final Window parent) {
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
}
