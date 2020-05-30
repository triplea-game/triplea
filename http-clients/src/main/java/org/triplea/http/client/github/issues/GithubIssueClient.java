package org.triplea.http.client.github.issues;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.error.report.ErrorReportRequest;

/**
 * Accumulates required args for making requests to create github issues, and then presents an API
 * to accept user upload data.
 */
public class GithubIssueClient {
  /**
   * Arbitrary length to prevent titles from being too large and cluttering up the issue display.
   */
  public static final int TITLE_MAX_LENGTH = 125;

  /** If this client is set to 'test' mode, we will return a stubbed response. */
  @VisibleForTesting
  static final String STUBBED_RETURN_VALUE =
      "API-token==test--returned-a-stubbed-github-issue-link";

  private final String githubOrg;
  private final String githubRepo;
  private final String authToken;
  private final GithubIssueFeignClient githubIssueFeignClient;
  /**
   * For local or integration testing, we may want to have a fake that does not actually call
   * github. This method returns true if we are doing a fake call to github.
   */
  private final boolean test;

  @Builder
  public GithubIssueClient(
      @Nonnull final URI uri,
      @Nonnull final String githubOrg,
      @Nonnull final String githubRepo,
      @Nonnull final String authToken,
      final boolean isTest) {
    githubIssueFeignClient = new HttpClient<>(GithubIssueFeignClient.class, uri).get();
    this.githubOrg = githubOrg;
    this.githubRepo = githubRepo;
    this.authToken = authToken;
    this.test = isTest;
  }

  /**
   * Invokes github web-API to create a github issue with the provided parameter data.
   *
   * @param uploadRequest Upload data for creating the body and title of the github issue.
   * @return Response from server containing link to the newly created issue.
   * @throws feign.FeignException thrown on error or if non-2xx response is received
   */
  public CreateIssueResponse newIssue(final ErrorReportRequest uploadRequest) {
    if (test) {
      return new CreateIssueResponse(STUBBED_RETURN_VALUE);
    }

    final Map<String, Object> tokens = new HashMap<>();
    tokens.put("Authorization", "token " + authToken);
    return githubIssueFeignClient.newIssue(
        tokens,
        githubOrg,
        githubRepo,
        CreateIssueRequest.builder()
            .title(uploadRequest.getTitle())
            .body(uploadRequest.getBody())
            .labels(new String[] {"Error Report"})
            .build());
  }
}
