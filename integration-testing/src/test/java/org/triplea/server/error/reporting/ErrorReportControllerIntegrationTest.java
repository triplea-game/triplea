package org.triplea.server.error.reporting;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.server.http.BasicEndpointTest;

class ErrorReportControllerIntegrationTest extends BasicEndpointTest<ErrorReportClient> {

  ErrorReportControllerIntegrationTest() {
    super(ErrorReportClient::newClient);
  }

  @Test
  void uploadErrorReport() {
    verifyEndpointReturningObject(
        client ->
            client.uploadErrorReport(
                SystemIdHeader.headers(),
                ErrorReportRequest.builder().body("body").title("title").build()));
  }
}
