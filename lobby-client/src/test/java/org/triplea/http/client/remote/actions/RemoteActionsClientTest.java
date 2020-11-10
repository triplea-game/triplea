package org.triplea.http.client.remote.actions;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.WireMockTest;
import org.triplea.java.IpAddressParser;
import ru.lanwen.wiremock.ext.WiremockResolver;

@SuppressWarnings("SameParameterValue")
class RemoteActionsClientTest extends WireMockTest {

  private static final String IPV4 = "99.55.99.0";

  @DisplayName("Wiremock test for 'isPlayerBanned'")
  @Nested
  class PlayerIsBanned {
    @Test
    void checkIfPlayerIsBanned(@WiremockResolver.Wiremock final WireMockServer server) {
      givenServerStubForIsPlayerBanned(IPV4, server, true);

      final boolean result =
          WireMockTest.newClient(server, RemoteActionsClient::new)
              .checkIfPlayerIsBanned(IpAddressParser.fromString(IPV4));

      assertThat(result, is(true));
    }

    private void givenServerStubForIsPlayerBanned(
        final String ip, final WireMockServer server, final boolean response) {
      server.stubFor(
          post(RemoteActionsClient.IS_PLAYER_BANNED_PATH)
              .withRequestBody(equalTo(ip))
              .willReturn(WireMock.aResponse().withStatus(200).withBody(String.valueOf(response))));
    }
  }

  @DisplayName("Wiremock test for 'sendShutdownRequest'")
  @Nested
  class SendShutdownRequest {
    @Test
    void sendShutdownRequest(@WiremockResolver.Wiremock final WireMockServer server) {
      givenServerStubForSendShutdownRequest("gameId", server);

      WireMockTest.newClient(server, RemoteActionsClient::new) //
          .sendShutdownRequest("gameId");
    }

    private void givenServerStubForSendShutdownRequest(
        final String gameId, final WireMockServer server) {
      server.stubFor(
          post(RemoteActionsClient.SEND_SHUTDOWN_PATH)
              .withRequestBody(equalTo(gameId))
              .willReturn(WireMock.aResponse().withStatus(200)));
    }
  }
}
