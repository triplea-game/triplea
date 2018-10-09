package org.triplea.http.client.github.issues;

import java.util.HashMap;
import java.util.Map;

import org.triplea.http.client.github.issues.create.CreateIssueRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

import com.google.common.annotations.VisibleForTesting;

import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

@SuppressWarnings("InterfaceNeverImplemented")
interface GithubIssueClient {

  @VisibleForTesting
  String CREATE_ISSUE_PATH = "/repos/{org}/{repo}/issues";

  @RequestLine("POST " + CREATE_ISSUE_PATH)
  @Headers({
      "Content-Type: application/json",
      "Accept: application/json"
  })
  CreateIssueResponse createIssue(
      final @HeaderMap Map<String, Object> headerMap,
      final @Param("org") String org,
      final @Param("repo") String repo,
      final CreateIssueRequest createIssueRequest);


  default CreateIssueResponse createIssue(
      final String authToken,
      final String org,
      final String repo,
      final CreateIssueRequest createIssueRequest) {

    final Map<String, Object> tokens = new HashMap<>();
    tokens.put("Authorization", "token " + authToken);
    return createIssue(tokens, org, repo, createIssueRequest);
  }
}
