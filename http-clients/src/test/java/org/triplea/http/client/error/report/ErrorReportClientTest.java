package org.triplea.http.client.error.report;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URI;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.test.common.Integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.gson.Gson;

import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
@Integration
class ErrorReportClientTest {
  private static final String MESSAGE_FROM_USER = "msg";
  private static final String LINK = "http://localhost";

  private static final ErrorUploadResponse SUCCESS_RESPONSE = ErrorUploadResponse.builder()
      .githubIssueLink(LINK)
      .build();

  @Test
  void sendErrorReportSuccessCase(@WiremockResolver.Wiremock final WireMockServer server) {
    final ErrorUploadResponse response =
        HttpClientTesting.sendServiceCallToWireMock(
            HttpClientTesting.ServiceCallArgs.<ErrorUploadResponse>builder()
                .wireMockServer(server)
                .expectedRequestPath(ErrorUploadClient.ERROR_REPORT_PATH)
                .expectedBodyContents(singletonList(MESSAGE_FROM_USER))
                .serverReturnValue(new Gson().toJson(SUCCESS_RESPONSE))
                .serviceCall(ErrorReportClientTest::doServiceCall)
                .build());

    assertThat(response, is(SUCCESS_RESPONSE));
  }

  private static ErrorUploadResponse doServiceCall(final URI hostUri) {
    return ErrorUploadClient.newClient(hostUri)
        .uploadErrorReport(ErrorUploadRequest.builder()
            .title("Guttuss cadunt in germanus oenipons!")
            .body(MESSAGE_FROM_USER)
            .build());
  }

  @Test
  void errorHandling(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    HttpClientTesting.verifyErrorHandling(
        wireMockServer, ErrorUploadClient.ERROR_REPORT_PATH, ErrorReportClientTest::doServiceCall);
  }


  @Test
  void canSubmitErrorReport(@WiremockResolver.Wiremock final WireMockServer server) {
    wireMockCanSubmitReport(server, true);
    final URI hostUri = URI.create(server.url(""));

    MatcherAssert.assertThat(
        ErrorUploadClient.newClient(hostUri).canSubmitErrorReport(),
        is(true));
  }

  private void wireMockCanSubmitReport(final WireMockServer wireMockServer, final boolean response) {
    wireMockServer.stubFor(
        WireMock.get(ErrorUploadClient.CAN_REPORT_PATH)
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(String.valueOf(response))));
  }

  @Test
  void canNotSubmitErrorReport(@WiremockResolver.Wiremock final WireMockServer server) {
    wireMockCanSubmitReport(server, false);
    final URI hostUri = URI.create(server.url(""));

    MatcherAssert.assertThat(
        ErrorUploadClient.newClient(hostUri).canSubmitErrorReport(),
        is(false));
  }
}
