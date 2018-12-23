package games.strategy.engine.framework.startup.ui.editors;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.common.base.Ascii;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.editors.validators.EmailValidator;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.ui.ProgressWindow;
import lombok.extern.java.Log;

/**
 * An editor for modifying email senders.
 */
@Log
public class EmailSenderEditor extends EditorPanel {
  private static final long serialVersionUID = -4647781117491269926L;
  private final JTextField subject = new JTextField();
  private final JTextField toAddress = new JTextField();
  private final JLabel toLabel = new JLabel("To:");
  private final JButton testEmail = new JButton("Test Email");
  private final JCheckBox alsoPostAfterCombatMove = new JCheckBox("Also Post After Combat Move");

  public EmailSenderEditor() {
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
    // add Test button on the same line as encryption
    add(testEmail, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
        new Insets(0, 0, bottomSpace, 0), 0, 0));
    testEmail.addActionListener(e -> testEmail());
    row++;
    add(alsoPostAfterCombatMove, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.NONE, new Insets(0, 0, bottomSpace, 0), 0, 0));
  }

  /**
   * Tests the email sender. This must be called from the swing event thread
   */
  private void testEmail() {
    final ProgressWindow progressWindow = GameRunner.newProgressWindow("Sending test email...");
    progressWindow.setVisible(true);
    // start a background thread
    new Thread(() -> {
      // initialize variables to error state, override if successful
      String message = "An unknown occurred, report this as a bug on the TripleA dev forum";
      int messageType = JOptionPane.ERROR_MESSAGE;
      try {
        final File dummy = new File(ClientFileSystemHelper.getUserRootFolder(), "dummySave.txt");
        dummy.deleteOnExit();
        try (OutputStream fout = new FileOutputStream(dummy)) {
          fout.write("This file would normally be a save game".getBytes(StandardCharsets.UTF_8));
        }
        final String html = "<html><body><h1>Success</h1><p>This was a test email sent by TripleA<p></body></html>";
        // ((IEmailSender) getBean()).sendEmail("TripleA Test", html, dummy, "dummy.txt");
        // email was sent, or an exception would have been thrown
        message = "Email sent, it should arrive shortly, otherwise check your spam folder";
        messageType = JOptionPane.INFORMATION_MESSAGE;
      } catch (final IOException ioe) {
        message = "Unable to send email, check SMTP server credentials: "
            + Ascii.truncate(ioe.getMessage(), 200, "...");
        log.log(Level.SEVERE, message, ioe);
      } finally {
        // now that we have a result, marshall it back unto the swing thread
        final String finalMessage = message;
        final int finalMessageType = messageType;
        SwingUtilities.invokeLater(() -> GameRunner.showMessageDialog(
            finalMessage,
            GameRunner.Title.of("Email Test"),
            finalMessageType));
        progressWindow.setVisible(false);
      }
    }).start();
  }

  public boolean areFieldsValid() {
    final boolean addressValid = validateTextField(toAddress, toLabel, new EmailValidator(false));
    testEmail.setEnabled(addressValid);
    return addressValid;
  }

  public void applyToGameProperties(final GameProperties properties) {
    properties.set(IEmailSender.SUBJECT, subject.getText());
    properties.set(IEmailSender.OPPONENT, toAddress.getText());
    properties.set(IEmailSender.POST_AFTER_COMBAT, alsoPostAfterCombatMove.isSelected());
  }

  public void populateFromGameProperties(final GameProperties properties) {
    subject.setText(properties.get(IEmailSender.SUBJECT, ""));
    toAddress.setText(properties.get(IEmailSender.OPPONENT, ""));
    alsoPostAfterCombatMove.setSelected(properties.get(IEmailSender.POST_AFTER_COMBAT, false));
  }
}
