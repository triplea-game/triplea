package games.strategy.debug.error.reporting;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.triplea.http.client.SendResult;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;

import lombok.Builder;

/**
 * Strategy object to upload an error report to http server.
 */
@Builder
class ErrorReportUploadAction implements Predicate<ErrorReport> {

  @Nonnull
  private final ServiceClient<ErrorReport, ErrorReportResponse> serviceClient;
  @Nonnull
  private final Consumer<URI> successConfirmation;
  @Nonnull
  private final Consumer<ServiceResponse<ErrorReportResponse>> failureConfirmation;


  @Override
  public boolean test(final ErrorReport errorReport) {
    final ServiceResponse<ErrorReportResponse> response = serviceClient.apply(errorReport);
    final URI githubLink =
        response.getPayload().map(pay -> pay.getGithubIssueLink().orElse(null)).orElse(null);

    if ((response.getSendResult() == SendResult.SENT) && (githubLink != null)) {
      successConfirmation.accept(githubLink);
      return true;
    } else {
      failureConfirmation.accept(response);
      return false;
    }
  }
}
