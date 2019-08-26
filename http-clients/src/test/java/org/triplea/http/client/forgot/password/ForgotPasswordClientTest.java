package org.triplea.http.client.forgot.password;

import static java.util.Arrays.asList;
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
class ForgotPasswordClientTest {
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
                .expectedBodyContents(asList(REQUEST.getUsername(), REQUEST.getEmail()))
                .serverReturnValue(new Gson().toJson(SUCCESS_RESPONSE))
                .serviceCall(ForgotPasswordClientTest::doServiceCall)
                .build());

    assertThat(response, is(SUCCESS_RESPONSE));
  }

  private static ForgotPasswordResponse doServiceCall(final URI hostUri) {
    return ForgotPasswordClient.newClient(hostUri).sendForgotPasswordRequest(REQUEST);
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
