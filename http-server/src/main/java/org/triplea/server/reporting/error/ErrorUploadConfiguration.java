package org.triplea.server.reporting.error;

import java.net.URI;

import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.github.issues.GithubIssueClientFactory;
import org.triplea.http.client.github.issues.IssueClientParams;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;
import org.triplea.server.EnvironmentVariable;

/** Class that handles object creation and dependencies for error upload. */
public final class ErrorUploadConfiguration {
  private ErrorUploadConfiguration() {}

  /**
   * Factory method for {@code ErrorUploader}.
   */
  public static ErrorUploadStrategy newErrorUploader() {
    final ServiceClient<CreateIssueRequest, CreateIssueResponse> createIssueClient =
        GithubIssueClientFactory.newGithubIssueCreator(
            IssueClientParams.builder()
                .authToken(EnvironmentVariable.GITHUB_API_AUTH_TOKEN.getValue())
                .githubOrg(EnvironmentVariable.ERROR_REPORTING_GITHUB_ORG.getValue())
                .githubRepo(EnvironmentVariable.ERROR_REPORTING_GITHUB_REPO.getValue())
                .uri(URI.create("https://api.github.com"))
                .build());

    return ErrorUploadStrategy.builder()
        .createIssueClient(createIssueClient)
        .requestAdapter(new ErrorReportRequestAdapter())
        .responseAdapter(new ErrorReportResponseAdapter())
        .allowErrorReport(new ErrorReportGateKeeper())
        .build();
  }
}
