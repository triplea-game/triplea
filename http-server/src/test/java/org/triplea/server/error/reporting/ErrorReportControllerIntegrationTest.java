package org.triplea.server.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.server.http.AbstractDropwizardTest;
import org.triplea.test.common.Integration;

@Integration
class ErrorReportControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ErrorUploadClient client =
      AbstractDropwizardTest.newClient(ErrorUploadClient::newClient);

  @Test
  void uploadErrorReport() {
    final ErrorUploadResponse response =
        client.uploadErrorReport(ErrorUploadRequest.builder().body("bodY").title("title").build());

    assertThat(response.getGithubIssueLink(), notNullValue());
  }

  @Test
  void canSubmitErrorReport() {
    assertThat(client.canSubmitErrorReport(), is(true));
  }
}
