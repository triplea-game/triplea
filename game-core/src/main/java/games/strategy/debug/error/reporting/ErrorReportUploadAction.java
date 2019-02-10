package games.strategy.debug.error.reporting;

import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.swing.JFrame;

import org.triplea.http.client.SendResult;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.swing.DialogBuilder;

import lombok.Builder;

@Builder
class ErrorReportUploadAction implements BiConsumer<JFrame, UserErrorReport> {

  static final BiConsumer<JFrame, UserErrorReport> OFFLINE_STRATEGY =
      (frame, report) -> DialogBuilder.builder()
          .parent(frame)
          .title("Unable to connect to server")
          .errorMessage(
              "TripleA is unable to get the servers network adddress, please restart "
                  + "Triplea and try again, if this problem keeps happening please contact Triplea")
          .showDialog();

  @Nonnull
  private final ServiceClient<ErrorReport, ErrorReportResponse> serviceClient;
  @Nonnull
  private final Consumer<URI> successConfirmation;
  @Nonnull
  private final Consumer<ServiceResponse<ErrorReportResponse>> failureConfirmation;


  @Override
  public void accept(final JFrame frame, final UserErrorReport errorReport) {
    final ServiceResponse<ErrorReportResponse> response = serviceClient.apply(errorReport.toErrorReport());
    final URI githubLink =
        response.getPayload().map(pay -> pay.getGithubIssueLink().orElse(null)).orElse(null);

    if ((response.getSendResult() == SendResult.SENT) && (githubLink != null)) {
      successConfirmation.accept(githubLink);
      frame.dispose();
    } else {
      failureConfirmation.accept(response);
      // We close the frame on success, but not on failure.
      // This is so the user can recover any data they have typed.
    }
  }
}
