package org.triplea.server;

import java.net.URI;

import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.lobby.server.EnvironmentVariable;
import org.triplea.server.reporting.error.CreateIssueStrategy;
import org.triplea.server.reporting.error.ErrorReportGateKeeper;
import org.triplea.server.reporting.error.ErrorReportResponseConverter;

import lombok.Builder;
import lombok.Getter;

/**
 * Dependency injection layer that wires together the business layer beans. These beans will be used
 * by controllers to then provide the functionality behind http endpoints.
 */
@Getter
@Builder
public class ServerConfiguration {
  private final CreateIssueStrategy errorUploader;

  public static ServerConfiguration fromEnvironmentVariables() {
    return builder().errorUploader(createIssueStrategy()).build();
  }

  private static CreateIssueStrategy createIssueStrategy() {
    return CreateIssueStrategy.builder()
        .createIssueClient(GithubIssueClient.builder()
            .authToken(EnvironmentVariable.GITHUB_API_AUTH_TOKEN.getValue())
            .githubOrg(EnvironmentVariable.ERROR_REPORTING_GITHUB_ORG.getValue())
            .githubRepo(EnvironmentVariable.ERROR_REPORTING_GITHUB_REPO.getValue())
            .uri(URI.create("https://api.github.com"))
            .build())
        .responseAdapter(new ErrorReportResponseConverter())
        .allowErrorReport(new ErrorReportGateKeeper())
        .build();
  }
}
