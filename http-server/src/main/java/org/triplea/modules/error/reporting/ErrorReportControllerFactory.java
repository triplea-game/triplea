package org.triplea.modules.error.reporting;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.AppConfig;
import org.triplea.http.client.github.issues.GithubIssueClient;

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

    if (githubIssueClient.isTest()) {
      Preconditions.checkState(!configuration.isProd());
    }

    return ErrorReportController.builder()
        .errorReportIngestion(CreateIssueStrategy.build(githubIssueClient, jdbi))
        .build();
  }
}
