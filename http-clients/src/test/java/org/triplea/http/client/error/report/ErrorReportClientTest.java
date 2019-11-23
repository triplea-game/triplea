package org.triplea.http.client.error.report;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gson.Gson;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ErrorReportClientTest extends WireMockTest {
  private static final String MESSAGE_FROM_USER = "msg";
  private static final String LINK = "http://localhost";

  private static final ErrorReportResponse SUCCESS_RESPONSE =
      ErrorReportResponse.builder().githubIssueLink(LINK).build();

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
            SystemIdHeader.headers(),
            ErrorReportRequest.builder()
                .title("Guttuss cadunt in germanus oenipons!")
                .body(MESSAGE_FROM_USER)
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
}
