package org.triplea.http.client.github;

import com.google.common.annotations.VisibleForTesting;
import feign.FeignException;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import java.util.List;
import java.util.Map;

@SuppressWarnings("InterfaceNeverImplemented")
interface GithubApiFeignClient {

  @VisibleForTesting String CREATE_ISSUE_PATH = "/repos/{org}/{repo}/issues";
  @VisibleForTesting String LIST_REPOS_PATH = "/orgs/{org}/repos";
  @VisibleForTesting String BRANCHES_PATH = "/repos/{org}/{repo}/branches/{branch}";
  @VisibleForTesting String LATEST_RELEASE_PATH = "/repos/{org}/{repo}/releases/latest";

  /**
   * Creates a new issue on github.com.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + CREATE_ISSUE_PATH)
  CreateIssueResponse newIssue(
      @Param("org") String org, @Param("repo") String repo, CreateIssueRequest createIssueRequest);

  @RequestLine("GET " + LIST_REPOS_PATH)
  List<MapRepoListing> listRepos(
      @QueryMap Map<String, String> queryParams, @Param("org") String org);

  @RequestLine("GET " + BRANCHES_PATH)
  BranchInfoResponse getBranchInfo(
      @Param("org") String org, @Param("repo") String repo, @Param("branch") String branch);

  @RequestLine("GET " + LATEST_RELEASE_PATH)
  LatestReleaseResponse getLatestRelease(@Param("org") String org, @Param("repo") String repo);
}
