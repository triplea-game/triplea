package org.triplea.server.error.reporting;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;
import org.triplea.lobby.server.db.dao.ErrorReportingDao;

import com.google.common.annotations.VisibleForTesting;

import lombok.Builder;
import lombok.NonNull;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class CreateIssueStrategy implements Function<ErrorReportRequest, ErrorUploadResponse> {

  @VisibleForTesting
  static final String STUBBED_RETURN_VALUE = "API-token==test--returned-a-stubbed-github-issue-link";

  @Nonnull
  private final Function<CreateIssueResponse, ErrorUploadResponse> responseAdapter;
  @Nonnull
  private final GithubIssueClient githubIssueClient;
  @Nonnull
  private final Predicate<String> allowErrorReport;
  /**
   * The 'production' flag is to help us verify we are not in a 'test' mode and will not return
   * stubbed values while in production.
   */
  @NonNull
  private final Boolean isProduction;
  @Nonnull
  private final ErrorReportingDao errorReportingDao;


  @Override
  public ErrorUploadResponse apply(final ErrorReportRequest errorReportRequest) {
    if (allowErrorReport.test(errorReportRequest.getClientIp())) {
      final ErrorUploadResponse errorUploadResponse = sendRequest(errorReportRequest);

      errorReportingDao.insertHistoryRecord(errorReportRequest.getClientIp());
      errorReportingDao.purgeOld(Instant.now().minus(2, ChronoUnit.DAYS));

      return errorUploadResponse;
    }

    throw new CreateErrorReportException("Error report limit reached, please wait a day to submit more.");
  }

  private ErrorUploadResponse sendRequest(final ErrorReportRequest errorReportRequest) {
    if (githubIssueClient.isTest()) {
      return ErrorUploadResponse.builder()
          .githubIssueLink(STUBBED_RETURN_VALUE)
          .build();
    }
    final CreateIssueResponse response =
        githubIssueClient.newIssue(errorReportRequest.getErrorReport());
    return responseAdapter.apply(response);
  }
}
