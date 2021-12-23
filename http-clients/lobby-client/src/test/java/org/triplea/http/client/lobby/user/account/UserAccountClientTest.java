package org.triplea.http.client.lobby.user.account;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class UserAccountClientTest extends WireMockTest {

  private static final String NEW_PASSWORD = "updated-password";
  private static final String EMAIL = "email";

  private static UserAccountClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, UserAccountClient::newClient);
  }

  @Test
  void updatePassword(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(UserAccountClient.CHANGE_PASSWORD_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(NEW_PASSWORD))
            .willReturn(WireMock.aResponse().withStatus(200)));
    newClient(server).changePassword(NEW_PASSWORD);
  }

  @Test
  void fetchPassword(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.get(UserAccountClient.FETCH_EMAIL_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(JsonUtil.toJson(new FetchEmailResponse(EMAIL)))));

    final String result = newClient(server).fetchEmail();

    assertThat(result, is(EMAIL));
  }

  @Test
  void changeEmail(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(UserAccountClient.CHANGE_EMAIL_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(EMAIL))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).changeEmail(EMAIL);
  }
}
