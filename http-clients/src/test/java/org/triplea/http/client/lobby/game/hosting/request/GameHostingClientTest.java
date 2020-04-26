package org.triplea.http.client.lobby.game.hosting.request;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class GameHostingClientTest extends WireMockTest {
  private static final GameHostingResponse GAME_HOSTING_RESPONSE =
      GameHostingResponse.builder().apiKey("key value").publicVisibleIp("127.9.9.9").build();

  private static GameHostingClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, GameHostingClient::newClient);
  }

  @Test
  void sendGameHostingRequest(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        post(GameHostingClient.GAME_HOSTING_REQUEST_PATH)
            .willReturn(
                WireMock.aResponse().withStatus(200).withBody(toJson(GAME_HOSTING_RESPONSE))));

    final GameHostingResponse result = newClient(wireMockServer).sendGameHostingRequest();

    assertThat(result, Is.is(GAME_HOSTING_RESPONSE));
  }
}
