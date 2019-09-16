package org.triplea.server.error.reporting;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.server.http.BasicEndpointTest;

class ErrorReportControllerIntegrationTest extends BasicEndpointTest<ErrorUploadClient> {

  ErrorReportControllerIntegrationTest() {
    super(ErrorUploadClient::newClient);
  }

  @Test
  void uploadErrorReport() {
    verifyEndpointReturningObject(
        client ->
            client.uploadErrorReport(
                ErrorUploadRequest.builder().body("body").title("title").build()));
  }

  @Test
  void canSubmitErrorReport() {
    verifyEndpointReturningObject(ErrorUploadClient::canSubmitErrorReport);
  }
}
