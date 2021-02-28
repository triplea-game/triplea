package org.triplea.http.client.lobby.login;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class LobbyLoginClientTest extends WireMockTest {

  private static final LoginRequest LOGIN_REQUEST =
      LoginRequest.builder().name("login_name").password("password_value").build();

  private static final LobbyLoginResponse SUCCESS_LOGIN_RESPONSE =
      LobbyLoginResponse.builder()
          .apiKey(WireMockTest.API_KEY.getValue())
          .passwordChangeRequired(true)
          .moderator(true)
          .build();

  private static final LobbyLoginResponse FAILED_LOGIN_RESPONSE =
      LobbyLoginResponse.builder().failReason("fail-reason").build();

  private static final CreateAccountRequest CREATE_ACCOUNT_REQUEST =
      CreateAccountRequest.builder()
          .username("username")
          .email("email")
          .password("password")
          .build();

  private static final CreateAccountResponse CREATE_ACCOUNT_SUCCESS_RESPONSE =
      CreateAccountResponse.SUCCESS_RESPONSE;

  private static final CreateAccountResponse CREATE_ACCOUNT_FAILURE_RESPONSE =
      CreateAccountResponse.builder().errorMessage("error").build();

  private static LobbyLoginClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, LobbyLoginClient::newClient);
  }

  @Test
  void loginSucces(@WiremockResolver.Wiremock final WireMockServer server) {
    givenLoginRequestReturning(server, SUCCESS_LOGIN_RESPONSE);

    final LobbyLoginResponse result = withLoginRequest(server);

    assertThat(result, is(SUCCESS_LOGIN_RESPONSE));
  }

  private void givenLoginRequestReturning(
      final WireMockServer server, final LobbyLoginResponse lobbyLoginResponse) {
    server.stubFor(
        post(LobbyLoginClient.LOGIN_PATH)
            .withRequestBody(equalToJson(JsonUtil.toJson(LOGIN_REQUEST)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(JsonUtil.toJson(lobbyLoginResponse))));
  }

  private LobbyLoginResponse withLoginRequest(final WireMockServer server) {
    return newClient(server).login(LOGIN_REQUEST.getName(), LOGIN_REQUEST.getPassword());
  }

  @Test
  void loginFailure(@WiremockResolver.Wiremock final WireMockServer server) {
    givenLoginRequestReturning(server, FAILED_LOGIN_RESPONSE);

    final LobbyLoginResponse result =
        newClient(server).login(LOGIN_REQUEST.getName(), LOGIN_REQUEST.getPassword());

    assertThat(result, is(FAILED_LOGIN_RESPONSE));
  }

  @Test
  void createAccount(@WiremockResolver.Wiremock final WireMockServer server) {
    givenCreateAccountRequestReturning(server, CREATE_ACCOUNT_SUCCESS_RESPONSE);

    final CreateAccountResponse result = withCreateAccountRequest(server);

    assertThat(result, is(CREATE_ACCOUNT_SUCCESS_RESPONSE));
  }

  private void givenCreateAccountRequestReturning(
      final WireMockServer server, final CreateAccountResponse createAccountResponse) {
    server.stubFor(
        post(LobbyLoginClient.CREATE_ACCOUNT)
            .withRequestBody(equalToJson(JsonUtil.toJson(CREATE_ACCOUNT_REQUEST)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(JsonUtil.toJson(createAccountResponse))));
  }

  private CreateAccountResponse withCreateAccountRequest(final WireMockServer server) {
    return newClient(server)
        .createAccount(
            CREATE_ACCOUNT_REQUEST.getUsername(),
            CREATE_ACCOUNT_REQUEST.getEmail(),
            CREATE_ACCOUNT_REQUEST.getPassword());
  }

  @Test
  void setCreateAccountFailureResponse(@WiremockResolver.Wiremock final WireMockServer server) {
    givenCreateAccountRequestReturning(server, CREATE_ACCOUNT_FAILURE_RESPONSE);

    final CreateAccountResponse result = withCreateAccountRequest(server);

    assertThat(result, is(CREATE_ACCOUNT_FAILURE_RESPONSE));
  }
}
