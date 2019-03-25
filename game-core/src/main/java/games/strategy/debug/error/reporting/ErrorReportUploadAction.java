package games.strategy.debug.error.reporting;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;

import feign.FeignException;
import lombok.Builder;

/**
 * Strategy object to upload an error report to http server.
 */
@Builder
class ErrorReportUploadAction implements Predicate<ErrorUploadRequest> {

  @Nonnull
  private final ErrorUploadClient serviceClient;
  @Nonnull
  private final Consumer<URI> successConfirmation;
  @Nonnull
  private final Consumer<FeignException> failureConfirmation;


  @Override
  public boolean test(final ErrorUploadRequest errorReport) {
    try {
      final ErrorUploadResponse response = serviceClient.uploadErrorReport(errorReport);
      successConfirmation.accept(URI.create(response.getGithubIssueLink()));
      return true;
    } catch (final FeignException e) {
      failureConfirmation.accept(e);
      return false;
    }
  }
}
