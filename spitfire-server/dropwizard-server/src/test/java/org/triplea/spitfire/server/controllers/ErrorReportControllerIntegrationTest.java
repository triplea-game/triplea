package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.CanUploadRequest;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.http.client.error.report.ErrorReportResponse;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class ErrorReportControllerIntegrationTest extends ControllerIntegrationTest {
  private final ErrorReportClient client;

  ErrorReportControllerIntegrationTest(final URI localhost) {
    client = ErrorReportClient.newClient(localhost);
  }

  @Test
  void uploadErrorReport() {
    final ErrorReportResponse response =
        client.uploadErrorReport(
            ErrorReportRequest.builder()
                .body("body")
                .title("error-report-title-" + String.valueOf(Math.random()).substring(0, 10))
                .gameVersion("version")
                .build());

    assertThat(response.getGithubIssueLink(), is(notNullValue()));
  }

  @Test
  void canUploadErrorReport() {
    client.canUploadErrorReport(
        CanUploadRequest.builder().gameVersion("2.0").errorTitle("title").build());
  }
}
