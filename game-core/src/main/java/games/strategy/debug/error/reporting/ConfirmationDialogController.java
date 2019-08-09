package games.strategy.debug.error.reporting;

import feign.FeignException;
import games.strategy.triplea.UrlConstants;
import java.net.URI;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import org.triplea.awt.OpenFileUtility;
import org.triplea.swing.JPanelBuilder;

/**
 * This controller is responsible for showing success/failure confirmation dialogs after an error
 * report has been submitted.
 */
final class ConfirmationDialogController {
  private ConfirmationDialogController() {}

  static void showFailureConfirmation(final FeignException exception) {
    SwingUtilities.invokeLater(() -> doShowFailureConfirmation(exception));
  }

  private static void doShowFailureConfirmation(final FeignException response) {
    final JEditorPane editorPane =
        new JEditorPane(
            "text/html",
            "Failure uploading report, please try again or <a href='"
                + UrlConstants.GITHUB_ISSUES
                + "'>contact support</a>.<br/><br/>"
                + response.getMessage());
    editorPane.setEditable(false);
    editorPane.setOpaque(false);
    editorPane.setBorder(new EmptyBorder(10, 0, 20, 0));
    editorPane.addHyperlinkListener(
        e -> {
          if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
            OpenFileUtility.openUrl(e.getURL().toString());
          }
        });

    final JPanel messageToShow = JPanelBuilder.builder().border(10).add(editorPane).build();

    // parentComponent == null to avoid pop-up from appearing behind other windows
    JOptionPane.showMessageDialog(
        null, messageToShow, "Report Upload Failed", JOptionPane.ERROR_MESSAGE);
  }

  static void showSuccessConfirmation(final URI reportLinkCreated) {
    SwingUtilities.invokeLater(() -> doShowSuccessConfirmation(reportLinkCreated));
  }

  private static void doShowSuccessConfirmation(final URI reportLinkCreated) {
    final JEditorPane editorPane =
        new JEditorPane(
            "text/html",
            String.format(
                "Upload success, report created:<br/><br/><a href='%s'>%s</a>",
                reportLinkCreated, reportLinkCreated));
    editorPane.setEditable(false);
    editorPane.setOpaque(false);
    editorPane.addHyperlinkListener(
        e -> {
          if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
            OpenFileUtility.openUrl(e.getURL().toString());
          }
        });

    final JPanel messageToShow = JPanelBuilder.builder().border(10).add(editorPane).build();

    // parentComponent == null to avoid pop-up from appearing behind other windows
    JOptionPane.showMessageDialog(
        null, messageToShow, "Report Uploaded Successfully", JOptionPane.INFORMATION_MESSAGE);
  }
}
