package org.triplea.http.client.github;

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

/** Can be used to interact with github's webservice API. */
public class GithubApiClient {

  /** If this client is set to 'test' mode, we will return a stubbed response. */
  @VisibleForTesting
  static final String STUBBED_RETURN_VALUE =
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
      return new CreateIssueResponse(STUBBED_RETURN_VALUE);
    }

    final Map<String, Object> tokens = buildAuthorizationHeaders();
    return githubApiFeignClient.newIssue(tokens, githubOrg, githubRepo, createIssueRequest);
  }

  private Map<String, Object> buildAuthorizationHeaders() {
    final Map<String, Object> tokens = new HashMap<>();
    // authToken is not required, can happen in dev environments.
    // Without an auth token the only consequence is the github API rate
    // limit is more strict.
    if (authToken != null && !authToken.isBlank()) {
      tokens.put("Authorization", "token " + authToken);
    }
    return tokens;
  }

  /**
   * Returns a listing of the repositories within a github organization. This call handles paging,
   * it returns a complete list and may perform multiple calls to Github.
   *
   * <p>Example equivalent cUrl call:
   *
   * <p>curl https://api.github.com/orgs/triplea-maps/repos
   */
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
    final Map<String, Object> tokens = buildAuthorizationHeaders();

    final Map<String, String> queryParams = new HashMap<>();
    queryParams.put("per_page", "100");
    queryParams.put("page", String.valueOf(pageNumber));

    return githubApiFeignClient.listRepos(tokens, queryParams, githubOrg).stream()
        .map(RepoListingResponse::getHtmlUrl)
        .map(URI::create)
        .collect(Collectors.toSet());
  }

  /**
   * Fetches details of a specific branch on a specific repo. Useful for retrieving info about the
   * last commit to the repo. Note, the repo listing contains a 'last_push' date, but this method
   * should be used instead as the last_push date on a repo can be for any branch (even PRs).
   *
   * <p>Example equivalent cUrl:
   * https://api.github.com/repos/triplea-maps/star_wars_galactic_war/branches/master
   *
   * @param org Name of the github org to be queried.
   * @param repo Name of the github repository.
   * @param branch Which branch to be queried.
   * @return Payload response object representing the response from Github's web API.
   */
  public BranchInfoResponse fetchBranchInfo(
      final String org, final String repo, final String branch) {
    final Map<String, Object> tokens = buildAuthorizationHeaders();
    return githubApiFeignClient.getBranchInfo(tokens, org, repo, branch);
  }
}
