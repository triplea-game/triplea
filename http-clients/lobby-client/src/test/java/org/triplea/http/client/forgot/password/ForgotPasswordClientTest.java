package org.triplea.http.client.forgot.password;

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

class ForgotPasswordClientTest extends WireMockTest {
  private static final ForgotPasswordRequest REQUEST =
      ForgotPasswordRequest.builder().username("user").email("email").build();

  private static final ForgotPasswordResponse SUCCESS_RESPONSE =
      ForgotPasswordResponse.builder().responseMessage("").build();

  @Test
  void sendForgotPasswordSuccessCase(@WiremockResolver.Wiremock final WireMockServer server) {
    final ForgotPasswordResponse response =
        HttpClientTesting.sendServiceCallToWireMock(
            HttpClientTesting.ServiceCallArgs.<ForgotPasswordResponse>builder()
                .wireMockServer(server)
                .expectedRequestPath(ForgotPasswordClient.FORGOT_PASSWORD_PATH)
                .expectedBodyContents(List.of(REQUEST.getUsername(), REQUEST.getEmail()))
                .serverReturnValue(new Gson().toJson(SUCCESS_RESPONSE))
                .serviceCall(ForgotPasswordClientTest::doServiceCall)
                .build());

    assertThat(response, is(SUCCESS_RESPONSE));
  }

  private static ForgotPasswordResponse doServiceCall(final URI hostUri) {
    return ForgotPasswordClient.newClient(hostUri)
        .sendForgotPasswordRequest(SystemIdHeader.headers(), REQUEST);
  }

  @Test
  void errorHandling(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    HttpClientTesting.verifyErrorHandling(
        wireMockServer,
        ForgotPasswordClient.FORGOT_PASSWORD_PATH,
        HttpClientTesting.RequestType.POST,
        ForgotPasswordClientTest::doServiceCall);
  }
}
