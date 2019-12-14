package org.triplea.debug.error.reporting;

import feign.FeignException;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;

/** Strategy object to upload an error report to http server. */
@Builder
class ErrorReportUploadAction implements Predicate<ErrorReportRequest> {

  @Nonnull private final ErrorReportClient serviceClient;
  @Nonnull private final Consumer<URI> successConfirmation;
  @Nonnull private final Consumer<FeignException> failureConfirmation;

  @Override
  public boolean test(final ErrorReportRequest errorReport) {
    try {
      final ErrorReportResponse response =
          serviceClient.uploadErrorReport(SystemIdHeader.headers(), errorReport);
      successConfirmation.accept(URI.create(response.getGithubIssueLink()));
      return true;
    } catch (final FeignException e) {
      failureConfirmation.accept(e);
      return false;
    }
  }
}
