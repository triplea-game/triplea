package org.triplea.server.error.reporting;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.server.http.BasicEndpointTest;

class ErrorReportControllerTest extends BasicEndpointTest<ErrorReportClient> {

  ErrorReportControllerTest() {
    super(ErrorReportClient::newClient);
  }

  @Test
  void uploadErrorReport() {
    super.verifyEndpointReturningObject(
        client ->
            client.uploadErrorReport(
                ErrorReportRequest.builder().body("body").title("title").build()));
  }
}
