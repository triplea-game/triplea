package org.triplea.debug.error.reporting;

import feign.FeignException;
import games.strategy.triplea.UrlConstants;
import java.net.URI;
import javax.swing.SwingUtilities;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.SwingComponents.DialogWithLinksParams;
import org.triplea.swing.SwingComponents.DialogWithLinksTypes;

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
    SwingComponents.showDialogWithLinks(
        DialogWithLinksParams.builder()
            .title("Report Upload Failed")
            .dialogType(DialogWithLinksTypes.ERROR)
            .dialogText(
                "Failure uploading report, please try again or <a href='"
                    + UrlConstants.GITHUB_ISSUES
                    + "'>contact support</a>.<br/><br/>"
                    + response.getMessage())
            .build());
  }

  static void showSuccessConfirmation(final URI reportLinkCreated) {
    SwingUtilities.invokeLater(() -> doShowSuccessConfirmation(reportLinkCreated));
  }

  private static void doShowSuccessConfirmation(final URI reportLinkCreated) {
    SwingComponents.showDialogWithLinks(
        DialogWithLinksParams.builder()
            .title("Report Uploaded Successfully")
            .dialogType(DialogWithLinksTypes.INFO)
            .dialogText(
                String.format(
                    "Upload success, report created:<br/><br/><a href='%s'>%s</a>",
                    reportLinkCreated, reportLinkCreated))
            .build());
  }
}
