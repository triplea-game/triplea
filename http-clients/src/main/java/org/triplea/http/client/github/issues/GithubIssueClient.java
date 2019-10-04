package org.triplea.http.client.github.issues;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

/**
 * Accumulates required args for making requests to create github issues, and then presents an API
 * to accept user upload data.
 */
public class GithubIssueClient {
  private final String githubOrg;
  private final String githubRepo;
  private final String authToken;
  private final GithubClient githubClient;

  @Builder
  public GithubIssueClient(
      @Nonnull final URI uri,
      @Nonnull final String githubOrg,
      @Nonnull final String githubRepo,
      @Nonnull final String authToken) {
    githubClient = GithubClient.newClient(uri);
    this.githubOrg = githubOrg;
    this.githubRepo = githubRepo;
    this.authToken = authToken;
  }

  /**
   * For local or integration testing, we may want to have a fake that does not actually call
   * github. This method returns true if we are doing a fake call to github.
   */
  public boolean isTest() {
    return authToken.equalsIgnoreCase("test");
  }

  /**
   * Invokes github web-API to create a github issue with the provided parameter data.
   *
   * @param uploadRequest Upload data for creating the body and title of the github issue.
   * @return Response from server containing link to the newly created issue.
   * @throws feign.FeignException thrown on error or if non-2xx response is received
   */
  public CreateIssueResponse newIssue(final ErrorReportRequest uploadRequest) {
    final Map<String, Object> tokens = new HashMap<>();
    tokens.put("Authorization", "token " + authToken);
    return githubClient.newIssue(tokens, githubOrg, githubRepo, uploadRequest);
  }
}
