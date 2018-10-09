package org.triplea.http.client.error.report;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.create.ErrorReportResponse;

class ErrorReportResponseTest {

  @Test
  void verifyErrorReportIdBehavior() {
    assertThat(
        ErrorReportResponse.builder()
            .githubIssueLink("")
            .build()
            .getGithubIssueLink(),
        isEmpty());
  }

  @Test
  void verify() {
    final String exampleValue = "https://myuri";
    assertThat(
        ErrorReportResponse.builder()
            .githubIssueLink(exampleValue)
            .build()
            .getGithubIssueLink(),
        isPresentAndIs(URI.create(exampleValue)));
  }


}
