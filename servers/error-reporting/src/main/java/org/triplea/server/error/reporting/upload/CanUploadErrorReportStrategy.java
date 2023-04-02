package org.triplea.server.error.reporting.upload;

import java.util.function.Function;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.error.report.CanUploadErrorReportResponse;
import org.triplea.http.client.error.report.CanUploadRequest;

/**
 * Answers the question if a user can upload an error report. If the given title and version already
 * exist, then a user cannot upload a (duplicate) error report.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CanUploadErrorReportStrategy
    implements Function<CanUploadRequest, CanUploadErrorReportResponse> {

  private final ErrorReportingDao errorReportingDao;

  public static CanUploadErrorReportStrategy build(final Jdbi jdbi) {
    return new CanUploadErrorReportStrategy(jdbi.onDemand(ErrorReportingDao.class));
  }

  @Override
  public CanUploadErrorReportResponse apply(final CanUploadRequest canUploadRequest) {
    return errorReportingDao
        .getErrorReportLink(canUploadRequest.getErrorTitle(), canUploadRequest.getGameVersion())
        .map(CanUploadErrorReportStrategy::buildResponseFromExisting)
        .orElseGet(
            () ->
                // no error report exists, user can upload
                CanUploadErrorReportResponse.builder().canUpload(true).build());
  }

  private static CanUploadErrorReportResponse buildResponseFromExisting(
      final String existingBugLink) {
    return CanUploadErrorReportResponse.builder()
        .canUpload(false)
        .existingBugReportUrl(existingBugLink)
        .responseDetails(
            "A bug report already exists for this problem. <br><br>"
                + "Please visit the bug report URL and add any details that could help.<br><br>"
                + "We are always interested to know how a problem happened<br>"
                + "and we may have questions you might be able to help answer.<br>")
        .build();
  }
}
