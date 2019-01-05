package games.strategy.debug.error.reporting;

import java.net.URI;
import java.util.function.BiConsumer;

import javax.swing.JFrame;

import org.triplea.http.client.SendResult;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;

import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import lombok.AllArgsConstructor;
import swinglib.DialogBuilder;

@AllArgsConstructor
class ErrorReportUploadAction implements BiConsumer<JFrame, UserErrorReport> {

  static final BiConsumer<JFrame, UserErrorReport> OFFLINE_STRATEGY =
      (frame, report) ->
          DialogBuilder.builder()
              .parent(frame)
              .title("Unable to connect to server")
              .errorMessage(
                  "TripleA is unable to get the servers network adddress, please restart "
                      + "Triplea and try again, if this problem keeps happening please contact Triplea")
              .showDialog();

  private final ServiceClient<ErrorReport, ErrorReportResponse> serviceClient;

  @Override
  public void accept(final JFrame frame, final UserErrorReport errorReport) {
    new BackgroundTaskRunner(frame)
        .awaitRunInBackground(
            "Sending report...", () -> sendErrorReport(frame, errorReport.toErrorReport()));
  }

  private void sendErrorReport(final JFrame frame, final ErrorReport errorReport) {
    final ServiceResponse<ErrorReportResponse> response = serviceClient.apply(errorReport);
    final URI githubLink =
        response.getPayload().map(pay -> pay.getGithubIssueLink().orElse(null)).orElse(null);

    if ((response.getSendResult() == SendResult.SENT) && (githubLink != null)) {
      DialogBuilder.builder()
          .parent(frame)
          .title("Report sent successfully")
          .infoMessage(
              "Upload success, report created: "
                  + response.getPayload().map(pay -> pay.getGithubIssueLink().orElse(null)))
          .showDialog();
      frame.dispose();
    } else {
      DialogBuilder.builder()
          .parent(frame)
          .title("Report upload failed")
          .errorMessage(errorMessage(response))
          .showDialog();
      // We close the frame on success, but not on failure.
      // This is so the user can recover any data they have typed.
    }
  }

  private static String errorMessage(final ServiceResponse<ErrorReportResponse> response) {
    return String.format(
        "<html>Failure uploading report, please try again or contact support. <br/>"
            + "Send result: %s<br />"
            + "Error: %s"
            + "</html>",
        response.getSendResult(), response.getExceptionMessage());
  }
}
