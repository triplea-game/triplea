package org.triplea.http.client.github;

import com.google.common.annotations.VisibleForTesting;
import feign.FeignException;
import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import java.util.List;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@SuppressWarnings("InterfaceNeverImplemented")
@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface GithubApiFeignClient {

  @VisibleForTesting String CREATE_ISSUE_PATH = "/repos/{org}/{repo}/issues";
  @VisibleForTesting String LIST_REPOS_PATH = "/orgs/{org}/repos";
  @VisibleForTesting String BRANCHES_PATH = "/repos/{org}/{repo}/branches/{branch}";

  /**
   * Creates a new issue on github.com.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + CREATE_ISSUE_PATH)
  CreateIssueResponse newIssue(
      @HeaderMap Map<String, Object> headerMap,
      @Param("org") String org,
      @Param("repo") String repo,
      CreateIssueRequest createIssueRequest);

  @RequestLine("GET " + LIST_REPOS_PATH)
  List<RepoListingResponse> listRepos(
      @HeaderMap Map<String, Object> headerMap,
      @QueryMap Map<String, String> queryParams,
      @Param("org") String org);

  @RequestLine("GET " + BRANCHES_PATH)
  BranchInfoResponse getBranchInfo(
      @HeaderMap Map<String, Object> headerMap,
      @Param("org") String org,
      @Param("repo") String repo,
      @Param("branch") String branch);
}
