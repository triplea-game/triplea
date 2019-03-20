package org.triplea.http.client.error.report;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;

class ErrorReportResponseTest {

  @Test
  void verifyErrorReportIdBehavior() {
    assertThat(
        ErrorUploadResponse.builder()
            .githubIssueLink("")
            .build()
            .getGithubIssueLink(),
        isEmpty());
  }

  @Test
  void verify() {
    final String exampleValue = "https://myuri";
    assertThat(
        ErrorUploadResponse.builder()
            .githubIssueLink(exampleValue)
            .build()
            .getGithubIssueLink(),
        isPresentAndIs(URI.create(exampleValue)));
  }


}
