package org.triplea.http.client.lobby.game.lobby.watcher;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.TestData;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class GameListingClientTest extends WireMockTest {

  private static final String GAME_ID = "gameId";

  private static final LobbyGame LOBBY_GAME = TestData.LOBBY_GAME;
  private static final LobbyGameListing LOBBY_GAME_LISTING =
      LobbyGameListing.builder().gameId(GAME_ID).lobbyGame(LOBBY_GAME).build();

  private static GameListingClient newClient(final WireMockServer wireMockServer) {
    return GameListingClient.newClient(buildHostUri(wireMockServer), API_KEY);
  }

  @Test
  void fetchGameListing(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        get(GameListingClient.FETCH_GAMES_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(JsonUtil.toJson(List.of(LOBBY_GAME_LISTING)))));

    final List<LobbyGameListing> results = newClient(server).fetchGameListing();

    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(LOBBY_GAME_LISTING));
  }

  @Test
  void bootGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(GameListingClient.BOOT_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(GAME_ID))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).bootGame(GAME_ID);
  }
}
