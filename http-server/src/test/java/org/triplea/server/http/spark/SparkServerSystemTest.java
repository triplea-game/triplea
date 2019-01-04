package org.triplea.server.http.spark;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.triplea.http.client.SendResult;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.ServiceResponse;
import org.triplea.http.client.error.report.ErrorReportClientFactory;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportDetails;
import org.triplea.http.client.error.report.create.ErrorReportResponse;
import org.triplea.server.ServerConfiguration;
import org.triplea.server.reporting.error.ErrorReportRequest;
import org.triplea.server.reporting.error.ErrorUploadStrategy;
import org.triplea.test.common.Integration;

import spark.Spark;


@Integration
class SparkServerSystemTest {

  private static final int SPARK_PORT = 5000;

  private static final URI LOCAL_HOST = URI.create("http://localhost:" + SPARK_PORT);

  private static final ErrorUploadStrategy errorUploadStrategy = Mockito.mock(ErrorUploadStrategy.class);

  @BeforeAll
  static void startServer() {
    Spark.port(SPARK_PORT);
    SparkServer.start(ServerConfiguration.builder()
        .errorUploader(errorUploadStrategy)
        .build());
    Spark.awaitInitialization();
  }

  @AfterEach
  void resetMock() {
    reset(errorUploadStrategy);
  }

  @AfterAll
  static void stopServer() {
    Spark.stop();
  }

  private static final ErrorReportRequest ERROR_REPORT =
      ErrorReportRequest.builder()
          .clientIp("")
          .errorReport(
              new ErrorReport(
                  ErrorReportDetails.builder()
                      .title("Amicitia pius mensa est.")
                      .description("Est brevis silva, cesaris.")
                      .gameVersion("test-version")
                      .build()))
          .build();

  private static final String LINK = "http://fictitious-link";

  @Test
  void errorReportEndpont() {
    final ServiceClient<ErrorReport, ErrorReportResponse> client =
        ErrorReportClientFactory.newErrorUploader(LOCAL_HOST);

    when(errorUploadStrategy.apply(ERROR_REPORT))
        .thenReturn(ErrorReportResponse.builder()
            .githubIssueLink(LINK)
            .build());

    final ServiceResponse<ErrorReportResponse> response =
        client.apply(ERROR_REPORT.getErrorReport());

    assertThat(response.getSendResult(), is(SendResult.SENT));
    assertThat(response.getPayload(), isPresent());
    assertThat(
        response.getPayload().get().getGithubIssueLink(),
        isPresentAndIs(URI.create(LINK)));
  }
}
