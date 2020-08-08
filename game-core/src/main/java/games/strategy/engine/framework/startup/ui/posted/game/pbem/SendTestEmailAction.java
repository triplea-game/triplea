package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import com.google.common.base.Ascii;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.posted.game.pbem.IEmailSender;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.java.Log;
import org.triplea.swing.ProgressWindow;

@Log
public class SendTestEmailAction {

  /** Tests the email sender. This must be called from the swing event thread */
  void send(final String to) {
    SwingUtilities.invokeLater(() -> sendWithProgressWindow(to));
  }

  private void sendWithProgressWindow(final String to) {
    final ProgressWindow progressWindow =
        new ProgressWindow(JOptionPane.getFrameForComponent(null), "Sending test email...");
    progressWindow.setVisible(true);
    new Thread(
            () -> {
              // initialize variables to error state, override if successful
              String message = "An unknown occurred, report this as a bug on the TripleA dev forum";
              int messageType = JOptionPane.ERROR_MESSAGE;
              try {
                final File dummy =
                    new File(ClientFileSystemHelper.getUserRootFolder(), "test-attachment.txt");
                dummy.deleteOnExit();
                try (var fileOutputStream = new FileOutputStream(dummy)) {
                  fileOutputStream.write(
                      "This file would normally be a save game".getBytes(StandardCharsets.UTF_8));
                }
                final String html =
                    "<html><body><h1>Success</h1><p>This was a test email "
                        + "sent by TripleA<p></body></html>";

                IEmailSender.newInstance("", to)
                    .sendEmail("TripleA Test", html, dummy, "dummy.txt");
                // email was sent, or an exception would have been thrown
                message = "Email sent, it should arrive shortly, otherwise check your spam folder";
                messageType = JOptionPane.INFORMATION_MESSAGE;
              } catch (final IOException ioe) {
                message =
                    "Unable to send email, check SMTP server credentials: "
                        + Ascii.truncate(ioe.getMessage(), 200, "...");
                log.log(Level.SEVERE, message, ioe);
              } finally {
                // now that we have a result, marshall it back unto the swing thread
                final String finalMessage = message;
                final int finalMessageType = messageType;
                SwingUtilities.invokeLater(
                    () ->
                        JOptionPane.showMessageDialog(
                            null, finalMessage, "Email Test", finalMessageType));
                progressWindow.setVisible(false);
              }
            })
        .start();
  }
}
