package org.triplea.http.client.lobby.game;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class ConnectivityCheckClientTest extends WireMockTest {
  private static final String GAME_ID = "Some game id";

  private static ConnectivityCheckClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, ConnectivityCheckClient::newClient);
  }

  @Test
  void verifyConnectivityCheck(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(ConnectivityCheckClient.CONNECTIVITY_CHECK_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(GAME_ID))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("true")));

    final boolean result = newClient(server).checkConnectivity(GAME_ID);

    assertThat(result, is(true));
  }
}
