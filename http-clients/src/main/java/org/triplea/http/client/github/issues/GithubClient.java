package org.triplea.http.client.github.issues;

import java.net.URI;
import java.util.Map;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.HttpConstants;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

import com.google.common.annotations.VisibleForTesting;

import feign.FeignException;
import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

@SuppressWarnings("InterfaceNeverImplemented")
interface GithubClient {

  @VisibleForTesting
  String CREATE_ISSUE_PATH = "/repos/{org}/{repo}/issues";

  /**
   * API for creating a new issue on github.com.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + CREATE_ISSUE_PATH)
  @Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
  CreateIssueResponse newIssue(
      @HeaderMap Map<String, Object> headerMap,
      @Param("org") String org,
      @Param("repo") String repo,
      ErrorUploadRequest errorReport);

  /** Creates an http client that can post a new github issue. */
  static GithubClient newClient(final URI githubApiHostUri) {
    return new HttpClient<>(GithubClient.class, githubApiHostUri).get();
  }
}
