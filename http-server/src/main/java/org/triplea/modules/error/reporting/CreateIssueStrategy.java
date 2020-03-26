package org.triplea.modules.error.reporting;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.ErrorReportingDao;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class CreateIssueStrategy
    implements BiFunction<String, ErrorReportRequest, ErrorReportResponse> {

  @VisibleForTesting
  static final String STUBBED_RETURN_VALUE =
      "API-token==test--returned-a-stubbed-github-issue-link";

  @Nonnull private final Function<CreateIssueResponse, ErrorReportResponse> responseAdapter;
  @Nonnull private final GithubIssueClient githubIssueClient;
  @Nonnull private final ErrorReportingDao errorReportingDao;

  public static CreateIssueStrategy build(
      final GithubIssueClient githubIssueClient, final Jdbi jdbi) {
    return CreateIssueStrategy.builder()
        .githubIssueClient(githubIssueClient)
        .responseAdapter(new ErrorReportResponseConverter())
        .errorReportingDao(jdbi.onDemand(ErrorReportingDao.class))
        .build();
  }

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
