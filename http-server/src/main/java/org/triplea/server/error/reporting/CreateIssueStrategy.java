package org.triplea.server.error.reporting;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;
import org.triplea.lobby.server.db.dao.ErrorReportingDao;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class CreateIssueStrategy
    implements BiFunction<String, ErrorReportRequest, ErrorReportResponse> {

  @VisibleForTesting
  static final String STUBBED_RETURN_VALUE =
      "API-token==test--returned-a-stubbed-github-issue-link";

  @Nonnull private final Function<CreateIssueResponse, ErrorReportResponse> responseAdapter;
  @Nonnull private final GithubIssueClient githubIssueClient;
  /**
   * The 'production' flag is to help us verify we are not in a 'test' mode and will not return
   * stubbed values while in production.
   */
  @NonNull private final Boolean isProduction;

  @Nonnull private final ErrorReportingDao errorReportingDao;

  @Override
  public ErrorReportResponse apply(
      final String ipAddress, final ErrorReportRequest errorReportRequest) {
    final ErrorReportResponse errorReportResponse = sendRequest(errorReportRequest);

    errorReportingDao.insertHistoryRecord(ipAddress);
    errorReportingDao.purgeOld(Instant.now().minus(365, ChronoUnit.DAYS));

    return errorReportResponse;
  }

  private ErrorReportResponse sendRequest(final ErrorReportRequest errorReportRequest) {
    if (githubIssueClient.isTest()) {
      return ErrorReportResponse.builder().githubIssueLink(STUBBED_RETURN_VALUE).build();
    }
    final CreateIssueResponse response = githubIssueClient.newIssue(errorReportRequest);
    return responseAdapter.apply(response);
  }
}
