package org.triplea.http.client.github.issues;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.WireMockTest;
import org.triplea.http.client.error.report.ErrorReportRequest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class GithubIssueClientTest extends WireMockTest {

  private static final String AUTH_TOKEN = "Where is the coal-black ship?";
  private static final String GITHUB_ORG = "Yo-ho-ho, yer not leading me without a malaria!";
  private static final String GITHUB_REPO = "Never crush a landlubber.";

  private static final String ISSUE_TITLE = "Gulls grow with madness.";
  private static final String ISSUE_BODY = "Belay, yer not loving me without a courage!";

  private static CreateIssueResponse doServiceCall(final URI hostUri) {
    return GithubIssueClient.builder()
        .authToken(AUTH_TOKEN)
        .githubOrg(GITHUB_ORG)
        .githubRepo(GITHUB_REPO)
        .uri(hostUri)
        .build()
        .newIssue(ErrorReportRequest.builder().title(ISSUE_TITLE).body(ISSUE_BODY).build());
  }

  @Test
  void server500(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    HttpClientTesting.verifyErrorHandling(
        wireMockServer,
        String.format("/repos/%s/%s/issues", GITHUB_ORG, GITHUB_REPO),
        HttpClientTesting.RequestType.POST,
        GithubIssueClientTest::doServiceCall);
  }
}
