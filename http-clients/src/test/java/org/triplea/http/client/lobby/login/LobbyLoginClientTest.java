package org.triplea.http.client.lobby.login;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gson.Gson;
import java.net.URI;
import java.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.error.report.ErrorReportClient;
import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

@ExtendWith({WiremockResolver.class, WiremockUriResolver.class})
class LobbyLoginClientTest {
  private static final LobbyLoginResponse SUCCESS_LOGIN =
      LobbyLoginResponse.newSuccessResponse("success");
  private static final LobbyLoginResponse FAILED_LOGIN =
      LobbyLoginResponse.newFailResponse("fail-reason");
  private static final String LOGIN_NAME = "example";
  private static final RegisteredUserLoginRequest REGISTERED_USER_LOGIN_REQUEST =
      RegisteredUserLoginRequest.builder().name("example").password("password").build();

  @Nested
  final class LoginTestCases {
    @Test
    void loginSuccess(@WiremockResolver.Wiremock final WireMockServer server) {
      final LobbyLoginResponse response =
          HttpClientTesting.sendServiceCallToWireMock(
              HttpClientTesting.ServiceCallArgs.<LobbyLoginResponse>builder()
                  .wireMockServer(server)
                  .expectedRequestPath(LobbyLoginClient.LOGIN_PATH)
                  .expectedBodyContents(
                      Arrays.asList(
                          REGISTERED_USER_LOGIN_REQUEST.getName(),
                          REGISTERED_USER_LOGIN_REQUEST.getPassword()))
                  .serverReturnValue(new Gson().toJson(SUCCESS_LOGIN))
                  .serviceCall(this::doServiceCall)
                  .build());

      assertThat(response, is(SUCCESS_LOGIN));
    }

    private LobbyLoginResponse doServiceCall(final URI hostUri) {
      return LobbyLoginClient.newClient(hostUri).login(REGISTERED_USER_LOGIN_REQUEST);
    }

    @Test
    void loginFailure(@WiremockResolver.Wiremock final WireMockServer server) {
      final LobbyLoginResponse response =
          HttpClientTesting.sendServiceCallToWireMock(
              HttpClientTesting.ServiceCallArgs.<LobbyLoginResponse>builder()
                  .wireMockServer(server)
                  .expectedRequestPath(LobbyLoginClient.LOGIN_PATH)
                  .expectedBodyContents(
                      Arrays.asList(
                          REGISTERED_USER_LOGIN_REQUEST.getName(),
                          REGISTERED_USER_LOGIN_REQUEST.getPassword()))
                  .serverReturnValue(new Gson().toJson(FAILED_LOGIN))
                  .serviceCall(this::doServiceCall)
                  .build());

      assertThat(response, is(FAILED_LOGIN));
    }

    @Test
    void errorHandling(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
      HttpClientTesting.verifyErrorHandling(
          wireMockServer,
          ErrorReportClient.ERROR_REPORT_PATH,
          HttpClientTesting.RequestType.POST,
          this::doServiceCall);
    }
  }

  @Nested
  final class AnonymousLoginCases {
    @Test
    void loginSuccess(@WiremockResolver.Wiremock final WireMockServer server) {
      final LobbyLoginResponse response =
          HttpClientTesting.sendServiceCallToWireMock(
              HttpClientTesting.ServiceCallArgs.<LobbyLoginResponse>builder()
                  .wireMockServer(server)
                  .expectedRequestPath(LobbyLoginClient.ANONYMOUS_LOGIN_PATH)
                  .expectedBodyContents(singletonList(LOGIN_NAME))
                  .serverReturnValue(new Gson().toJson(SUCCESS_LOGIN))
                  .serviceCall(this::doServiceCall)
                  .build());

      assertThat(response, is(SUCCESS_LOGIN));
    }

    private LobbyLoginResponse doServiceCall(final URI hostUri) {
      return LobbyLoginClient.newClient(hostUri).anonymousLogin(LOGIN_NAME);
    }

    @Test
    void loginFailure(@WiremockResolver.Wiremock final WireMockServer server) {
      final LobbyLoginResponse response =
          HttpClientTesting.sendServiceCallToWireMock(
              HttpClientTesting.ServiceCallArgs.<LobbyLoginResponse>builder()
                  .wireMockServer(server)
                  .expectedRequestPath(LobbyLoginClient.ANONYMOUS_LOGIN_PATH)
                  .expectedBodyContents(singletonList(LOGIN_NAME))
                  .serverReturnValue(new Gson().toJson(FAILED_LOGIN))
                  .serviceCall(this::doServiceCall)
                  .build());

      assertThat(response, is(FAILED_LOGIN));
    }

    @Test
    void errorHandling(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
      HttpClientTesting.verifyErrorHandling(
          wireMockServer,
          ErrorReportClient.ERROR_REPORT_PATH,
          HttpClientTesting.RequestType.POST,
          this::doServiceCall);
    }
  }
}
