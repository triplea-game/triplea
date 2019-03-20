package org.triplea.http.client.error.report;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URI;
import java.util.Arrays;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.ServiceResponse;
import org.triplea.test.common.Integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;

import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

/**
 * A test that checks the http client works, we use wiremock to simulate a server so we are not coupled
 * to any one server implementation. Server sub-projects should include the http-client as a test dependency
 * to then create an integration test to be sure that everything would work. Meanwhile we can test here
 * against a generic/stubbed server to be sure the client contract works as expected.
 */
@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
@Integration
class ErrorReportClientTest {
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String MESSAGE_FROM_USER = "msg";
  private static final String LINK = "http://localhost";

  @Test
  void sendErrorReportSuccessCase(@WiremockResolver.Wiremock final WireMockServer server) {
    givenHttpServerSuccessResponse(server);

    final ServiceResponse<ErrorUploadResponse> response = doServiceCall(server);

    verify(postRequestedFor(urlMatching(ErrorUploadClient.ERROR_REPORT_PATH))
        .withRequestBody(containing(MESSAGE_FROM_USER))
        .withHeader(HttpHeaders.CONTENT_TYPE, matching(CONTENT_TYPE_JSON)));

    assertThat(response.getPayload(), isPresent());
    assertThat(response.getPayload().get().getError(), is(""));
    assertThat(response.getPayload().get().getGithubIssueLink().get(), is(URI.create(LINK)));
    assertThat(response.getThrown(), isEmpty());
  }


  private static void givenHttpServerSuccessResponse(final WireMockServer wireMockServer) {
    wireMockServer.stubFor(post(urlEqualTo(ErrorUploadClient.ERROR_REPORT_PATH))
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody(String.format("{ \"error\":\"%s\", githubIssueLink=\"%s\" }",
                "", LINK))));
  }

  private static ServiceResponse<ErrorUploadResponse> doServiceCall(final WireMockServer wireMockServer) {
    WireMock.configureFor("localhost", wireMockServer.port());
    final URI hostUri = URI.create(wireMockServer.url(""));
    return ErrorUploadClientFactory.newErrorUploader(hostUri)
        .apply(ErrorUploadRequest.builder()
            .title("Guttuss cadunt in germanus oenipons!")
            .body(MESSAGE_FROM_USER)
            .build());
  }

  @Test
  void communicationFaultCases(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    Arrays.asList(
        // caution, one of the wiremock faults is known to cause a hang in windows, so to aviod that
        // problem do not use the full available list of of wiremock faults
        Fault.EMPTY_RESPONSE,
        Fault.RANDOM_DATA_THEN_CLOSE)
        .forEach(fault -> testFaultHandling(wireMockServer, fault));
  }

  private static void testFaultHandling(final WireMockServer wireMockServer, final Fault fault) {
    givenFaultyConnection(wireMockServer, fault);

    final ServiceResponse<ErrorUploadResponse> response = doServiceCall(wireMockServer);

    assertThat(response.getPayload(), isEmpty());
    assertThat(response.getThrown(), isPresent());
    assertThat(response.getExceptionMessage(), isPresent());
  }

  private static void givenFaultyConnection(final WireMockServer wireMockServer, final Fault fault) {
    wireMockServer.stubFor(post(urlEqualTo(ErrorUploadClient.ERROR_REPORT_PATH))
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withFault(fault)
            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody("a simulated error occurred")));
  }

  @Test
  void server500(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    givenServer500(wireMockServer);

    final ServiceResponse<ErrorUploadResponse> response = doServiceCall(wireMockServer);

    assertThat(response.getPayload(), isEmpty());
    assertThat(response.getThrown(), isPresent());
    assertThat(response.getExceptionMessage(), isPresent());
  }

  private static void givenServer500(final WireMockServer wireMockServer) {
    wireMockServer.stubFor(post(urlEqualTo(ErrorUploadClient.ERROR_REPORT_PATH))
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody("{ \"result\":\"FAILURE\" }")));
  }
}
