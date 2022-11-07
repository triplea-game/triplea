package org.triplea.http.client.github;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import feign.FeignException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.LoggerFactory;
import org.triplea.http.client.HttpClient;

/** Can be used to interact with github's webservice API. */
public class GithubApiClient {
  /** If this client is set to 'test' mode, we will return a stubbed response. */
  @VisibleForTesting
  static final String STUBBED_RETURN_VALUE =
      "API-token==test--returned-a-stubbed-github-issue-link";

  private final GithubApiFeignClient githubApiFeignClient;
  /**
   * Flag useful for testing, when set to true no API calls will be made and a hardcoded stubbed
   * value of {@code STUBBED_RETURN_VALUE} will always be returned.
   */
  private final boolean stubbingModeEnabled;

  @Getter private final String org;
  private final String repo;

  /**
   * @param uri The URI for githubs webservice API.
   * @param authToken Auth token that will be sent to Github for webservice calls. Can be empty, but
   *     if specified must be valid (no auth token still works, but rate limits will be more
   *     restrictive).
   * @param org Name of the github org to be queried.
   * @param repo Name of the github repository, may be left null if using only 'org' level APIs (EG:
   *     list repositories)
   * @param stubbingModeEnabled When set to true, stub values will be returned and the github web
   *     API will not actually be contacted.
   */
  @Builder
  public GithubApiClient(
      @Nonnull URI uri,
      @Nonnull String authToken,
      @Nonnull String org,
      String repo,
      final boolean stubbingModeEnabled) {
    githubApiFeignClient =
        HttpClient.newClient(
            GithubApiFeignClient.class,
            uri,
            Strings.isNullOrEmpty(authToken)
                ? Map.of()
                : Map.of("Authorization", "token " + authToken));
    this.stubbingModeEnabled = stubbingModeEnabled;
    this.org = org;
    this.repo = repo;
  }

  /**
   * Invokes github web-API to create a github issue with the provided parameter data.
   *
   * @param createIssueRequest Upload data for creating the body and title of the github issue.
   * @return Response from server containing link to the newly created issue.
   * @throws feign.FeignException thrown on error or if non-2xx response is received
   */
  public CreateIssueResponse newIssue(final CreateIssueRequest createIssueRequest) {
    Preconditions.checkNotNull(repo);
    if (stubbingModeEnabled) {
      return new CreateIssueResponse(
          STUBBED_RETURN_VALUE + String.valueOf(Math.random()).substring(0, 5));
    }

    return githubApiFeignClient.newIssue(org, repo, createIssueRequest);
  }

  /**
   * Returns a listing of the repositories within a github organization. This call handles paging,
   * it returns a complete list and may perform multiple calls to Github.
   *
   * <p>Example equivalent cUrl call:
   *
   * <p>curl https://api.github.com/orgs/triplea-maps/repos
   */
  public Collection<MapRepoListing> listRepositories() {
    final Collection<MapRepoListing> allRepos = new HashSet<>();
    int pageNumber = 1;
    Collection<MapRepoListing> repos = listRepositories(pageNumber);
    while (!repos.isEmpty()) {
      pageNumber++;
      allRepos.addAll(repos);
      repos = listRepositories(pageNumber);
    }
    return allRepos;
  }

  private Collection<MapRepoListing> listRepositories(int pageNumber) {
    final Map<String, String> queryParams = new HashMap<>();
    queryParams.put("per_page", "100");
    queryParams.put("page", String.valueOf(pageNumber));

    return githubApiFeignClient.listRepos(queryParams, org);
  }

  /**
   * Fetches details of a specific branch on a specific repo. Useful for retrieving info about the
   * last commit to the repo. Note, the repo listing contains a 'last_push' date, but this method
   * should be used instead as the last_push date on a repo can be for any branch (even PRs).
   *
   * <p>Example equivalent cUrl:
   * https://api.github.com/repos/triplea-maps/star_wars_galactic_war/branches/master
   *
   * @param branch Which branch to be queried.
   * @return Payload response object representing the response from Github's web API.
   */
  public BranchInfoResponse fetchBranchInfo(String branch) {
    return fetchBranchInfo(repo, branch);
  }

  public BranchInfoResponse fetchBranchInfo(String repo, String branch) {
    Preconditions.checkNotNull(repo);
    return githubApiFeignClient.getBranchInfo(org, repo, branch);
  }

  public Optional<String> fetchLatestVersion() {
    Preconditions.checkNotNull(repo);
    try {
      return Optional.of(githubApiFeignClient.getLatestRelease(org, repo).getTagName());
    } catch (final FeignException e) {
      LoggerFactory.getLogger(GithubApiClient.class)
          .warn("No data received from server for latest engine version", e);
      return Optional.empty();
    }
  }
}
