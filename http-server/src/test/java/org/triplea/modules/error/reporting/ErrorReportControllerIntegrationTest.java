package org.triplea.modules.error.reporting;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.modules.http.BasicEndpointTest;

class ErrorReportControllerIntegrationTest extends BasicEndpointTest<ErrorReportClient> {

  ErrorReportControllerIntegrationTest() {
    super(ErrorReportClient::newClient);
  }

  @Test
  void uploadErrorReport() {
    verifyEndpointReturningObject(
        client ->
            client.uploadErrorReport(
                ErrorReportRequest.builder().body("body").title("title").build()));
  }
}
