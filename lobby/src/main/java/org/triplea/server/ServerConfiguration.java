package org.triplea.server;

import java.net.URI;
import java.util.function.Function;

import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.http.client.github.issues.GithubIssueClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.RegisteredUserLoginRequest;
import org.triplea.lobby.server.EnvironmentVariable;
import org.triplea.server.reporting.error.CreateIssueStrategy;
import org.triplea.server.reporting.error.ErrorReportGateKeeper;
import org.triplea.server.reporting.error.ErrorReportRequest;
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

  private final Function<ErrorReportRequest, ErrorUploadResponse> errorUploader;
  private final Function<RegisteredUserLoginRequest, LobbyLoginResponse> registeredUserLogin;
  private final Function<String, LobbyLoginResponse> anonymousUserLogin;

  public static ServerConfiguration fromEnvironmentVariables() {
    return builder()
        .errorUploader(createIssueStrategy())
        .registeredUserLogin(registeredUserLoginStrategy())
        .anonymousUserLogin(anonymousUserLoginStrategy())
        .build();
  }

  private static Function<ErrorReportRequest, ErrorUploadResponse> createIssueStrategy() {
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


  private static Function<RegisteredUserLoginRequest, LobbyLoginResponse> registeredUserLoginStrategy() {
    // TODO: stubbed value, implement this;
    return loginRequest -> LobbyLoginResponse.newFailResponse("stubbed response");
  }

  private static Function<String, LobbyLoginResponse> anonymousUserLoginStrategy() {
    // TODO: stubbed value, implement this;
    return loginRequest -> LobbyLoginResponse.newFailResponse("stubbed response");
  }
}
