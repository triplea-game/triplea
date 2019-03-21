package org.triplea.server.http.spark;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.server.reporting.error.CreateErrorReportException;

import feign.FeignException;

class ErrorReportControllerTest extends SparkServerSystemTest {
  private static final org.triplea.server.reporting.error.ErrorReportRequest ERROR_REPORT =
      org.triplea.server.reporting.error.ErrorReportRequest.builder()
          .clientIp("")
          .errorReport(
              ErrorUploadRequest.builder()
                  .title("Ah there's nothing like the salty endurance stuttering on the plunder.")
                  .body("Where is the shiny jack?")
                  .build())
          .build();
  private static final String LINK = "http://fictitious-link";

  private final ErrorUploadClient client = ErrorUploadClient.newClient(LOCAL_HOST);

  @Test
  void errorReportEndpont() {
    when(errorUploadStrategy.apply(Mockito.any()))
        .thenReturn(ErrorUploadResponse.builder()
            .githubIssueLink(LINK)
            .build());

    final ErrorUploadResponse response = client.uploadErrorReport(ERROR_REPORT.getErrorReport());

    assertThat(response.getGithubIssueLink(), is(LINK));
  }

  @Test
  void serverFailsToCreateReport() {
    when(errorUploadStrategy.apply(Mockito.any()))
        .thenThrow(new CreateErrorReportException("simulated exception"));

    assertThrows(
        FeignException.class,
        () -> client.uploadErrorReport(ERROR_REPORT.getErrorReport()));
  }
}
