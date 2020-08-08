package org.triplea.http.client.github.issues;

import com.google.common.annotations.VisibleForTesting;
import feign.FeignException;
import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.Map;
import org.triplea.http.client.HttpConstants;

@SuppressWarnings("InterfaceNeverImplemented")
@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
interface GithubIssueFeignClient {

  @VisibleForTesting String CREATE_ISSUE_PATH = "/repos/{org}/{repo}/issues";

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
}
