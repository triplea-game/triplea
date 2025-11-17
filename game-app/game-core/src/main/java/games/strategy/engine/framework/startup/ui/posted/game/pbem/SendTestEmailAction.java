package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.posted.game.pbem.IEmailSender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.StringUtils;
import org.triplea.java.ThreadRunner;
import org.triplea.swing.ProgressWindow;

@Slf4j
public class SendTestEmailAction {

  /** Tests the email sender. This must be called from the swing event thread */
  void send(final String to) {
    SwingUtilities.invokeLater(() -> sendWithProgressWindow(to));
  }

  private void sendWithProgressWindow(final String to) {
    final ProgressWindow progressWindow =
        new ProgressWindow(JOptionPane.getFrameForComponent(null), "Sending test email...");
    progressWindow.setVisible(true);
    ThreadRunner.runInNewThread(
        () -> {
          // initialize variables to error state, override if successful
          String message = "An unknown occurred, report this as a bug on the TripleA dev forum";
          int messageType = JOptionPane.ERROR_MESSAGE;
          try {
            final Path dummy =
                ClientFileSystemHelper.getUserRootFolder().resolve("test-attachment.txt");
            dummy.toFile().deleteOnExit();
            Files.writeString(dummy, "This file would normally be a save game");
            final String html =
                "<html><body><h1>Success</h1><p>This was a test email "
                    + "sent by TripleA<p></body></html>";

            IEmailSender.newInstance("", to).sendEmail("TripleA Test", html, dummy, "dummy.txt");
            // email was sent, or an exception would have been thrown
            message = "Email sent, it should arrive shortly, otherwise check your spam folder";
            messageType = JOptionPane.INFORMATION_MESSAGE;
          } catch (final IOException ioe) {
            message =
                "Unable to send email, check SMTP server credentials: "
                    + StringUtils.truncate(ioe.getMessage(), 200);
            log.error(message, ioe);
          } finally {
            // now that we have a result, marshall it back unto the swing thread
            final String finalMessage = message;
            final int finalMessageType = messageType;
            SwingUtilities.invokeLater(
                () ->
                    JOptionPane.showMessageDialog(
                        progressWindow, finalMessage, "Email Test", finalMessageType));
            progressWindow.setVisible(false);
          }
        });
  }
}
