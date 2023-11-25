package org.triplea.server.error.reporting.upload;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.CreateIssueRequest;
import org.triplea.http.client.github.GithubApiClient;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class ErrorReportModule {
  @Nonnull private final GithubApiClient githubApiClient;
  @Nonnull private final ErrorReportingDao errorReportingDao;

  public static ErrorReportModule build(GithubApiClient githubApiClient, Jdbi jdbi) {
    return ErrorReportModule.builder()
        .githubApiClient(githubApiClient)
        .errorReportingDao(jdbi.onDemand(ErrorReportingDao.class))
        .build();
  }

  /**
   * Creates an error report (a github issue), records in database the created issue, and purges
   * very old error reports from database.
   */
  public ErrorReportResponse createErrorReport(CreateIssueParams createIssueParams) {
    var errorReportRequest = createIssueParams.getErrorReportRequest();

    var githubCreateIssueResponse =
        githubApiClient.newIssue(
            CreateIssueRequest.builder()
                .title(errorReportRequest.getTitle())
                .body(errorReportRequest.getBody())
                .labels(new String[] {"Error Report", errorReportRequest.getSimpleGameVersion()})
                .build());

    errorReportingDao.insertHistoryRecord(
        InsertHistoryRecordParams.builder()
            .ip(createIssueParams.getIp())
            .systemId(createIssueParams.getSystemId())
            .gameVersion(createIssueParams.getErrorReportRequest().getGameVersion())
            .title(createIssueParams.getErrorReportRequest().getTitle())
            .githubIssueLink(githubCreateIssueResponse.getHtmlUrl())
            .build());
    errorReportingDao.purgeOld(Instant.now().minus(365, ChronoUnit.DAYS));

    return new ErrorReportResponse(githubCreateIssueResponse.getHtmlUrl());
  }
}
