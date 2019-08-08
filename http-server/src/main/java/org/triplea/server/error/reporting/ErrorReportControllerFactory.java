package org.triplea.server.error.reporting;

import com.google.common.base.Preconditions;
import java.time.Clock;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.lobby.server.db.dao.ErrorReportingDao;
import org.triplea.server.http.AppConfig;

/** Http controller that binds the error upload endpoint with the error report upload handler. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorReportControllerFactory {
  /** Creates a {@code ErrorReportController} with dependencies. */
  public static ErrorReportController buildController(
      final AppConfig configuration, final Jdbi jdbi) {
    final GithubIssueClient githubIssueClient =
        GithubIssueClient.builder()
            .uri(AppConfig.GITHUB_WEB_SERVICE_API_URL)
            .authToken(configuration.getGithubApiToken())
            .githubOrg(AppConfig.GITHUB_ORG)
            .githubRepo(configuration.getGithubRepo())
            .build();

    final ErrorReportingDao errorReportingDao = jdbi.onDemand(ErrorReportingDao.class);
    final Predicate<String> errorReportGateKeeper =
        ErrorReportGateKeeper.builder()
            .maxReportsPerDay(AppConfig.MAX_ERROR_REPORTS_PER_DAY)
            .dao(errorReportingDao)
            .clock(Clock.systemUTC())
            .build();

    if (githubIssueClient.isTest()) {
      Preconditions.checkState(!configuration.isProd());
    }

    return ErrorReportController.builder()
        .errorReportIngestion(
            CreateIssueStrategy.builder()
                .githubIssueClient(githubIssueClient)
                .allowErrorReport(errorReportGateKeeper)
                .responseAdapter(new ErrorReportResponseConverter())
                .isProduction(configuration.isProd())
                .errorReportingDao(errorReportingDao)
                .build())
        .errorReportRateChecker(errorReportGateKeeper)
        .build();
  }
}
