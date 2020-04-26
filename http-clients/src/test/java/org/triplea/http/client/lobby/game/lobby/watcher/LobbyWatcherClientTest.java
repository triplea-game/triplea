package org.triplea.http.client.lobby.game.lobby.watcher;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.TestData;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class LobbyWatcherClientTest extends WireMockTest {

  private static final String GAME_ID = "gameId";

  private static final LobbyGame LOBBY_GAME = TestData.LOBBY_GAME;

  private static LobbyWatcherClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, LobbyWatcherClient::newClient);
  }

  @Test
  void postGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(LobbyWatcherClient.POST_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalToJson(toJson(LOBBY_GAME)))
            .willReturn(WireMock.aResponse().withStatus(200).withBody(GAME_ID)));

    final String gameId = newClient(server).postGame(LOBBY_GAME);

    assertThat(gameId, is(GAME_ID));
  }

  @Test
  void updateGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(LobbyWatcherClient.UPDATE_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(
                equalToJson(
                    toJson(
                        UpdateGameRequest.builder().gameId(GAME_ID).gameData(LOBBY_GAME).build())))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).updateGame(GAME_ID, LOBBY_GAME);
  }

  @Test
  void sendKeepAlive(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(LobbyWatcherClient.KEEP_ALIVE_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(GAME_ID))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("true")));

    final boolean result = newClient(server).sendKeepAlive(GAME_ID);

    assertThat(result, is(true));
  }

  @Test
  void removeGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(LobbyWatcherClient.REMOVE_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(GAME_ID))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).removeGame(GAME_ID);
  }
}
