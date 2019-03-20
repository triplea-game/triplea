package games.strategy.debug.error.reporting;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.triplea.http.client.SendResult;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;

import lombok.Builder;

/**
 * Strategy object to upload an error report to http server.
 */
@Builder
class ErrorReportUploadAction implements Predicate<ErrorUploadRequest> {

  @Nonnull
  private final ServiceClient<ErrorUploadRequest, ErrorUploadResponse> serviceClient;
  @Nonnull
  private final Consumer<URI> successConfirmation;
  @Nonnull
  private final Consumer<ServiceResponse<ErrorUploadResponse>> failureConfirmation;


  @Override
  public boolean test(final ErrorUploadRequest errorReport) {
    final ServiceResponse<ErrorUploadResponse> response = serviceClient.apply(errorReport);
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
