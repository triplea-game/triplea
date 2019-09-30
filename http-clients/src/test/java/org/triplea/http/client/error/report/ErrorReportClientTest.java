package org.triplea.http.client.error.report;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gson.Gson;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClientTesting;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({WiremockResolver.class, WiremockUriResolver.class})
class ErrorReportClientTest {
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
                .expectedBodyContents(singletonList(MESSAGE_FROM_USER))
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
