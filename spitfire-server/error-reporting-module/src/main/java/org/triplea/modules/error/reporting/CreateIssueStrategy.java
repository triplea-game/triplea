package org.triplea.modules.error.reporting;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.http.client.github.CreateIssueRequest;
import org.triplea.http.client.github.GithubApiClient;
import org.triplea.modules.error.reporting.db.ErrorReportingDao;
import org.triplea.modules.error.reporting.db.InsertHistoryRecordParams;

/** Performs the steps for uploading an error report from the point of view of the server. */
@Builder
public class CreateIssueStrategy {
  @Nonnull private final GithubApiClient githubApiClient;
  @Nonnull private final ErrorReportingDao errorReportingDao;
  @Nonnull private final String githubOrg;
  @Nonnull private final String githubRepo;

  public static CreateIssueStrategy build(
      final String githubOrg,
      final String githubRepo,
      final GithubApiClient githubApiClient,
      final Jdbi jdbi) {
    return CreateIssueStrategy.builder()
        .githubOrg(githubOrg)
        .githubRepo(githubRepo)
        .githubApiClient(githubApiClient)
        .errorReportingDao(jdbi.onDemand(ErrorReportingDao.class))
        .build();
  }

  public ErrorReportResponse createGithubIssue(final CreateIssueParams createIssueParams) {
    var errorReportRequest = createIssueParams.getErrorReportRequest();

    var githubCreateIssueResponse =
        githubApiClient.newIssue(
            githubOrg,
            githubRepo,
            CreateIssueRequest.builder()
                .title(errorReportRequest.getGameVersion() + ": " + errorReportRequest.getTitle())
                .body(errorReportRequest.getBody())
                .labels(new String[] {"Error Report"})
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
