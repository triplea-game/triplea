package org.triplea.http.client.github.issues;

import java.util.HashMap;
import java.util.Map;

import org.triplea.http.client.error.report.ErrorUploadRequest;
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
  CreateIssueResponse newIssue(
      @HeaderMap Map<String, Object> headerMap,
      @Param("org") String org,
      @Param("repo") String repo,
      ErrorUploadRequest errorReport);


  default CreateIssueResponse newIssue(
      final IssueClientParams params,
      final ErrorUploadRequest errorReport) {
    final Map<String, Object> tokens = new HashMap<>();
    tokens.put("Authorization", "token " + params.getAuthToken());
    return newIssue(tokens, params.getGithubOrg(), params.getGithubRepo(), errorReport);
  }
}
