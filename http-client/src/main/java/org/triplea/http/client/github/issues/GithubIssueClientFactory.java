package org.triplea.http.client.github.issues;

import java.net.URI;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.github.issues.create.CreateIssueRequest;
import org.triplea.http.client.github.issues.create.CreateIssueResponse;

/**
 * Creates an http client that can be used to interact with Github 'issues'.
 */
public class GithubIssueClientFactory {

  /**
   * Creates an http client that can post a new github issue.
   *
   * @param authToken Github Personal access token with repo permissions
   * @param githubOrg The name of the github org, used as part of URL.
   * @param githubRepo The name of the github repo, used as part of URL
   */
  public ServiceClient<CreateIssueRequest, CreateIssueResponse> newGithubIssueCreator(
      final String authToken,
      final String githubOrg,
      final String githubRepo,
      final URI uri) {
    return new ServiceClient<>(
        new HttpClient<>(
            GithubIssueClient.class,
            (client, request) -> client.newIssue(authToken, githubOrg, githubRepo, request),
            uri));
  }
}
