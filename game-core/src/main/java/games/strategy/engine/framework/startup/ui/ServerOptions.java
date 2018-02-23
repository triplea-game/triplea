package games.strategy.engine.framework.startup.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingAction;

/**
 * UI for choosing server options.
 */
public class ServerOptions extends JDialog {
  private static final long serialVersionUID = -9074816386666798281L;
  private JTextField nameField;
  private IntTextField portField;
  private JPasswordField passwordField;
  private boolean okPressed;
  private JCheckBox requirePasswordCheckBox;
  private JTextField comment;
  private boolean showComment = false;

  /**
   * Creates a new instance of ServerOptions.
   */
  public ServerOptions(final Component owner, final String defaultName, final int defaultPort,
      final boolean showComment) {
    super((owner == null) ? null : JOptionPane.getFrameForComponent(owner), "Server options", true);
    this.showComment = showComment;
    initComponents();
    layoutComponents();
    setupActions();
    nameField.setText(defaultName);
    portField.setValue(defaultPort);
    setWidgetActivation();
    pack();
  }

  public void setNameEditable(final boolean editable) {
    nameField.setEditable(editable);
  }

  private void setupActions() {
    requirePasswordCheckBox.addActionListener(e -> setWidgetActivation());
  }

  @Override
  public String getName() {
    // fixes crash by truncating names to 20 characters
    final String s = nameField.getText().trim();
    if (s.length() > 20) {
      return s.substring(0, 20);
    }
    return s;
  }

  public String getPassword() {
    if (!requirePasswordCheckBox.isSelected()) {
      return null;
    }
    final String password = new String(passwordField.getPassword());
    if (password.trim().length() == 0) {
      return null;
    }
    return password;
  }

  public int getPort() {
    return portField.getValue();
  }

  private void initComponents() {
    nameField = new JTextField(10);
    portField = new IntTextField(0, Integer.MAX_VALUE);
    portField.setColumns(7);
    passwordField = new JPasswordField();
    passwordField.setColumns(10);
    comment = new JTextField();
    comment.setColumns(20);
  }

  private void layoutComponents() {
    final Container content = getContentPane();
    content.setLayout(new BorderLayout());
    final JPanel title = new JPanel();
    title.add(new JLabel("Select server options"));
    content.add(title, BorderLayout.NORTH);
    final Insets labelSpacing = new Insets(3, 7, 0, 0);
    final Insets fieldSpacing = new Insets(3, 5, 0, 7);
    final GridBagConstraints labelConstraints = new GridBagConstraints();
    labelConstraints.anchor = GridBagConstraints.WEST;
    labelConstraints.gridx = 0;
    labelConstraints.insets = labelSpacing;
    final GridBagConstraints fieldConstraints = new GridBagConstraints();
    fieldConstraints.anchor = GridBagConstraints.WEST;
    fieldConstraints.gridx = 1;
    fieldConstraints.insets = fieldSpacing;
    requirePasswordCheckBox = new JCheckBox("");
    final JLabel passwordRequiredLabel = new JLabel("Require Password:");
    final JPanel fields = new JPanel();
    final GridBagLayout layout = new GridBagLayout();
    fields.setLayout(layout);
    final JLabel nameLabel = new JLabel("Name:");
    final JLabel portLabel = new JLabel("Port:");
    final JLabel passwordLabel = new JLabel("Password:");
    final JLabel commentLabel = new JLabel("Comments:");
    layout.setConstraints(portLabel, labelConstraints);
    layout.setConstraints(nameLabel, labelConstraints);
    layout.setConstraints(passwordLabel, labelConstraints);
    layout.setConstraints(portField, fieldConstraints);
    layout.setConstraints(nameField, fieldConstraints);
    layout.setConstraints(passwordField, fieldConstraints);
    layout.setConstraints(requirePasswordCheckBox, fieldConstraints);
    layout.setConstraints(passwordRequiredLabel, labelConstraints);
    fields.add(nameLabel);
    fields.add(nameField);
    fields.add(portLabel);
    fields.add(portField);
    fields.add(passwordRequiredLabel);
    fields.add(requirePasswordCheckBox);
    fields.add(passwordLabel);
    fields.add(passwordField);
    if (showComment) {
      layout.setConstraints(commentLabel, labelConstraints);
      layout.setConstraints(comment, fieldConstraints);
      fields.add(commentLabel);
      fields.add(comment);
    }
    content.add(fields, BorderLayout.CENTER);
    final JPanel buttons = new JPanel();
    buttons.add(new JButton(SwingAction.of("OK", e -> {
      setVisible(false);
      okPressed = true;
    })));
    buttons.add(new JButton(SwingAction.of("Cancel", e -> setVisible(false))));
    content.add(buttons, BorderLayout.SOUTH);
  }

  public boolean getOkPressed() {
    return okPressed;
  }

  private void setWidgetActivation() {
    passwordField.setEnabled(requirePasswordCheckBox.isSelected());
    final Color backGround = passwordField.isEnabled() ? portField.getBackground() : getBackground();
    passwordField.setBackground(backGround);
  }

  public String getComments() {
    return comment.getText();
  }
}
