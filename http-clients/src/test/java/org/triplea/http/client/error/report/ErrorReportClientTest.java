package org.triplea.http.client.error.report;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ErrorReportClientTest extends WireMockTest {
  private static final String MESSAGE_FROM_USER = "msg";
  private static final String LINK = "http://localhost";

  private static final ErrorReportResponse SUCCESS_RESPONSE =
      ErrorReportResponse.builder().githubIssueLink(LINK).build();

  private static final CanUploadErrorReportResponse CAN_UPLOAD_ERROR_REPORT_RESPONSE =
      CanUploadErrorReportResponse.builder()
          .responseDetails("details")
          .canUpload(true)
          .existingBugReportUrl("url")
          .build();

  private static ErrorReportClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ErrorReportClient::newClient);
  }

  @Test
  void sendErrorReportSuccessCase(@WiremockResolver.Wiremock final WireMockServer server) {
    final ErrorReportResponse response =
        HttpClientTesting.sendServiceCallToWireMock(
            HttpClientTesting.ServiceCallArgs.<ErrorReportResponse>builder()
                .wireMockServer(server)
                .expectedRequestPath(ErrorReportClient.ERROR_REPORT_PATH)
                .expectedBodyContents(List.of(MESSAGE_FROM_USER))
                .serverReturnValue(new Gson().toJson(SUCCESS_RESPONSE))
                .serviceCall(ErrorReportClientTest::doServiceCall)
                .build());

    assertThat(response, is(SUCCESS_RESPONSE));
  }

  private static ErrorReportResponse doServiceCall(final URI hostUri) {
    return ErrorReportClient.newClient(hostUri)
        .uploadErrorReport(
            ErrorReportRequest.builder()
                .title("Guttuss cadunt in germanus oenipons!")
                .body(MESSAGE_FROM_USER)
                .gameVersion("version")
                .build());
  }

  @Test
  void errorHandling(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    HttpClientTesting.verifyErrorHandling(
        wireMockServer,
        ErrorReportClient.ERROR_REPORT_PATH,
        HttpClientTesting.RequestType.POST,
        ErrorReportClientTest::doServiceCall);
  }

  @Test
  void canUploadErrorReport(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        post(ErrorReportClient.CAN_UPLOAD_ERROR_REPORT_PATH)
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(toJson(CAN_UPLOAD_ERROR_REPORT_RESPONSE))));

    final CanUploadErrorReportResponse response =
        newClient(wireMockServer)
            .canUploadErrorReport(
                CanUploadRequest.builder().gameVersion("2.0").errorTitle("title").build());

    assertThat(response, is(CAN_UPLOAD_ERROR_REPORT_RESPONSE));
  }
}
