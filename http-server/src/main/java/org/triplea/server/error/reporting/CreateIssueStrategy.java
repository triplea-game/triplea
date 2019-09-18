package org.triplea.server.error.reporting;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;
import org.triplea.lobby.server.db.dao.ErrorReportingDao;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class CreateIssueStrategy implements Function<ErrorReportRequest, ErrorUploadResponse> {

  @VisibleForTesting
  static final String STUBBED_RETURN_VALUE =
      "API-token==test--returned-a-stubbed-github-issue-link";

  @Nonnull private final Function<CreateIssueResponse, ErrorUploadResponse> responseAdapter;
  @Nonnull private final GithubIssueClient githubIssueClient;
  /**
   * The 'production' flag is to help us verify we are not in a 'test' mode and will not return
   * stubbed values while in production.
   */
  @NonNull private final Boolean isProduction;

  @Nonnull private final ErrorReportingDao errorReportingDao;

  @Override
  public ErrorUploadResponse apply(final ErrorReportRequest errorReportRequest) {
    final ErrorUploadResponse errorUploadResponse = sendRequest(errorReportRequest);

    errorReportingDao.insertHistoryRecord(errorReportRequest.getClientIp());
    errorReportingDao.purgeOld(Instant.now().minus(365, ChronoUnit.DAYS));

    return errorUploadResponse;
  }

  private ErrorUploadResponse sendRequest(final ErrorReportRequest errorReportRequest) {
    if (githubIssueClient.isTest()) {
      return ErrorUploadResponse.builder().githubIssueLink(STUBBED_RETURN_VALUE).build();
    }
    final CreateIssueResponse response =
        githubIssueClient.newIssue(errorReportRequest.getErrorReport());
    return responseAdapter.apply(response);
  }
}
