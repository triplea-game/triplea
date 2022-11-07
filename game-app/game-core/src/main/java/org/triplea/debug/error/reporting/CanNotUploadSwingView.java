package org.triplea.debug.error.reporting;

import java.util.Optional;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.error.report.CanUploadErrorReportResponse;
import org.triplea.swing.JEditorPaneWithClickableLinks;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Shows a dialog that gives a message to user about why they cannot report an error (usually
 * because it already exists). If a link to an error report is provided in the server response, this
 * dialog renders a clickable link the user can click to open the bug report.
 */
@UtilityClass
class CanNotUploadSwingView {

  static void showView(
      final JFrame parent, final CanUploadErrorReportResponse canUploadErrorReportResponse) {

    JOptionPane.showMessageDialog(
        parent,
        new JPanelBuilder()
            .border(10)
            .add(
                new JEditorPaneWithClickableLinks(
                    createWindowTextContents(canUploadErrorReportResponse)))
            .build(),
        "",
        JOptionPane.INFORMATION_MESSAGE);
  }

  private static String createWindowTextContents(
      final CanUploadErrorReportResponse reportResponse) {
    return reportResponse.getResponseDetails()
        + "<br>"
        + Optional.ofNullable(reportResponse.getExistingBugReportUrl())
            .map(url -> JEditorPaneWithClickableLinks.toLink(url, url))
            .orElse("");
  }
}
