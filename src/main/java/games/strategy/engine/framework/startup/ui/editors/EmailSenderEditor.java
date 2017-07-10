package games.strategy.engine.framework.startup.ui.editors;

import java.awt.GridBagConstraints;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.editors.validators.EmailValidator;
import games.strategy.engine.framework.startup.ui.editors.validators.IntegerRangeValidator;
import games.strategy.engine.pbem.GenericEmailSender;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.ui.ProgressWindow;

/**
 * An editor for modifying email senders.
 */
public class EmailSenderEditor extends EditorPanel {
  private static final long serialVersionUID = -4647781117491269926L;
  private final GenericEmailSender genericEmailSender;
  private final JTextField subject = new JTextField();
  private final JTextField toAddress = new JTextField();
  private final JTextField host = new JTextField();
  private final JTextField port = new JTextField();
  private final JTextField login = new JTextField();
  private final JCheckBox useTls = new JCheckBox("Use TLS encryption");
  private final JTextField password = new JPasswordField();
  private final JLabel toLabel = new JLabel("To:");
  private final JLabel hostLabel = new JLabel("Host:");
  private final JLabel portLabel = new JLabel("Port:");
  private final JButton testEmail = new JButton("Test Email");
  private final JCheckBox alsoPostAfterCombatMove = new JCheckBox("Also Post After Combat Move");
  private final JCheckBox credentialsSaved = new JCheckBox("Remember me");

  /**
   * creates a new instance.
   *
   * @param bean
   *        the EmailSender to edit
   * @param editorConfiguration
   *        configures which editor fields should be visible
   */
  public EmailSenderEditor(final GenericEmailSender bean, final EditorConfiguration editorConfiguration) {
    super();
    genericEmailSender = bean;
    subject.setText(genericEmailSender.getSubjectPrefix());
    host.setText(genericEmailSender.getHost());
    port.setText(String.valueOf(genericEmailSender.getPort()));
    toAddress.setText(genericEmailSender.getToAddress());
    login.setText(genericEmailSender.getUserName());
    password.setText(genericEmailSender.getPassword());
    credentialsSaved.setSelected(genericEmailSender.areCredentialsSaved());
    useTls.setSelected(genericEmailSender.getEncryption() == GenericEmailSender.Encryption.TLS);

    final int bottomSpace = 1;
    final int labelSpace = 2;
    int row = 0;
    add(new JLabel("Subject:"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(subject, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    add(toLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(toAddress, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    final JLabel loginLabel = new JLabel("Login:");
    add(loginLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(login, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    final JLabel passwordLabel = new JLabel("Password:");
    add(passwordLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(password, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    row++;
    add(new JLabel(""), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
    add(credentialsSaved, new GridBagConstraints(1, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
    if (editorConfiguration.showHost) {
      row++;
      add(hostLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
      add(host, new GridBagConstraints(1, row, 2, 1, 1.0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, bottomSpace, 0), 0, 0));
    }
    if (editorConfiguration.showPort) {
      row++;
      add(portLabel, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, labelSpace), 0, 0));
      add(port, new GridBagConstraints(1, row, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, bottomSpace, 0), 0, 0));
    }
    if (editorConfiguration.showEncryption) {
      row++;
      add(useTls, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, 0), 0, 0));
      // add Test button on the same line as encryption
      add(testEmail, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, 0), 0, 0));
      row++;
      add(alsoPostAfterCombatMove, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST,
          GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
    } else {
      row++;
      add(alsoPostAfterCombatMove, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST,
          GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
      add(testEmail, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(0, 0, bottomSpace, 0), 0, 0));
      // or on a separate line if no encryption
      // add(testEmail, new GridBagConstraints(1, row, 3, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
      // new Insets(0, 0,
      // bottomSpace, 0), 0, 0));
    }
    setupListeners();
  }

  private void setupListeners() {
    final EditorChangedFiringDocumentListener listener = new EditorChangedFiringDocumentListener();
    host.getDocument().addDocumentListener(listener);
    login.getDocument().addDocumentListener(listener);
    port.getDocument().addDocumentListener(listener);
    password.getDocument().addDocumentListener(listener);
    toAddress.getDocument().addDocumentListener(listener);
    credentialsSaved.addActionListener(e -> fireEditorChanged());
    useTls.addActionListener(e -> fireEditorChanged());
    testEmail.addActionListener(e -> testEmail());
  }

  /**
   * Tests the email sender. This must be called from the swing event thread
   */
  private void testEmail() {
    final ProgressWindow progressWindow = GameRunner.newProgressWindow("Sending test email...");
    progressWindow.setVisible(true);
    final Runnable runnable = () -> {
      // initialize variables to error state, override if successful
      String message = "An unknown occurred, report this as a bug on the TripleA dev forum";
      int messageType = JOptionPane.ERROR_MESSAGE;
      try {
        final String html = "<html><body><h1>Success</h1><p>This was a test email sent by TripleA<p></body></html>";
        final File dummy = new File(ClientFileSystemHelper.getUserRootFolder(), "dummySave.txt");
        dummy.deleteOnExit();
        final FileOutputStream fout = new FileOutputStream(dummy);
        fout.write("This file would normally be a save game".getBytes());
        fout.close();
        ((IEmailSender) getBean()).sendEmail("TripleA Test", html, dummy, "dummy.txt");
        // email was sent, or an exception would have been thrown
        message = "Email sent, it should arrive shortly, otherwise check your spam folder";
        messageType = JOptionPane.INFORMATION_MESSAGE;
      } catch (final IOException ioe) {
        message = "Unable to send email: " + ioe.getMessage();
      } finally {
        // now that we have a result, marshall it back unto the swing thread
        final String finalMessage = message;
        final int finalMessageType = messageType;
        SwingUtilities.invokeLater(() -> {
          try {
            GameRunner.showMessageDialog(
                finalMessage,
                GameRunner.Title.of("Email Test"),
                finalMessageType);
          } catch (final HeadlessException e) {
            // should never happen in a GUI app
          }
        });
        progressWindow.setVisible(false);
      }
    };
    // start a background thread
    final Thread t = new Thread(runnable);
    t.start();
  }

  @Override
  public boolean isBeanValid() {
    final boolean hostValid = validateTextFieldNotEmpty(host, hostLabel);
    final boolean portValid = validateTextField(port, portLabel, new IntegerRangeValidator(0, 65635));
    // boolean loginValid = validateTextFieldNotEmpty(login, loginLabel);
    // boolean passwordValid = validateTextFieldNotEmpty(password, passwordLabel);
    final boolean addressValid = validateTextField(toAddress, toLabel, new EmailValidator(false));
    final boolean allValid = hostValid && portValid && /* loginValid && passwordValid && */addressValid;
    testEmail.setEnabled(allValid);
    return allValid;
  }

  @Override
  public IBean getBean() {
    genericEmailSender
        .setEncryption(useTls.isSelected() ? GenericEmailSender.Encryption.TLS : GenericEmailSender.Encryption.NONE);
    genericEmailSender.setSubjectPrefix(subject.getText());
    genericEmailSender.setHost(host.getText());
    genericEmailSender.setUserName(login.getText());
    genericEmailSender.setPassword(password.getText());
    genericEmailSender.setCredentialsSaved(credentialsSaved.isSelected());
    int port = 0;
    try {
      port = Integer.parseInt(this.port.getText());
    } catch (final NumberFormatException e) {
      // ignore
    }
    genericEmailSender.setPort(port);
    genericEmailSender.setToAddress(toAddress.getText());
    genericEmailSender.setAlsoPostAfterCombatMove(alsoPostAfterCombatMove.isSelected());
    return genericEmailSender;
  }

  /**
   * class for configuring the editor so some fields can be hidden.
   */
  public static class EditorConfiguration {
    public boolean showHost;
    public boolean showPort;
    public boolean showEncryption;

    public EditorConfiguration() {}

    public EditorConfiguration(final boolean showHost, final boolean showPort, final boolean showEncryption) {
      this.showHost = showHost;
      this.showPort = showPort;
      this.showEncryption = showEncryption;
    }
  }
}
