package org.triplea.http.client.github.issues;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.HttpClient;

/**
 * Accumulates required args for making requests to create github issues, and then presents an API
 * to accept user upload data.
 */
public class GithubApiClient {

  /**
   * If this client is set to 'test' mode, we will return a stubbed response when creating issues.
   */
  @VisibleForTesting
  static final String CREATE_ISSUE_STUBBED_RETURN_VALUE =
      "API-token==test--returned-a-stubbed-github-issue-link";

  private final String authToken;
  private final GithubApiFeignClient githubApiFeignClient;
  /**
   * For local or integration testing, we may want to have a fake that does not actually call
   * github. This method returns true if we are doing a fake call to github.
   */
  private final boolean test;

  @Builder
  public GithubApiClient(
      @Nonnull final URI uri, @Nonnull final String authToken, final boolean isTest) {
    githubApiFeignClient = new HttpClient<>(GithubApiFeignClient.class, uri).get();
    this.authToken = authToken;
    this.test = isTest;
  }

  /**
   * Invokes github web-API to create a github issue with the provided parameter data.
   *
   * @param createIssueRequest Upload data for creating the body and title of the github issue.
   * @return Response from server containing link to the newly created issue.
   * @throws feign.FeignException thrown on error or if non-2xx response is received
   */
  public CreateIssueResponse newIssue(
      final String githubOrg,
      final String githubRepo,
      final CreateIssueRequest createIssueRequest) {
    if (test) {
      return new CreateIssueResponse(CREATE_ISSUE_STUBBED_RETURN_VALUE);
    }

    final Map<String, Object> tokens = new HashMap<>();
    tokens.put("Authorization", "token " + authToken);
    return githubApiFeignClient.newIssue(tokens, githubOrg, githubRepo, createIssueRequest);
  }

  public Collection<URI> listRepositories(final String githubOrg) {
    final Collection<URI> allRepos = new HashSet<>();
    int pageNumber = 1;
    Collection<URI> repos = listRepositories(githubOrg, pageNumber);
    while (!repos.isEmpty()) {
      pageNumber++;
      allRepos.addAll(repos);
      repos = listRepositories(githubOrg, pageNumber);
    }
    return allRepos;
  }

  private Collection<URI> listRepositories(final String githubOrg, final int pageNumber) {
    final Map<String, Object> tokens = new HashMap<>();
//    tokens.put("Authorization", "token " + authToken);
    final Map<String, String> queryParams = new HashMap<>();
    queryParams.put("per_page", "100");
    queryParams.put("page", String.valueOf(pageNumber));

    return githubApiFeignClient.listRepos(tokens, queryParams, githubOrg).stream()
        .map(RepoListingResponse::getHtmlUrl)
        .map(URI::create)
        .collect(Collectors.toSet());
  }
}
