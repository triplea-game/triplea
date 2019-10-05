package org.triplea.http.client.lobby.game.listing;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClientTesting;
import org.triplea.http.client.WireMockTest;
import ru.lanwen.wiremock.ext.WiremockResolver;

class GameListingClientTest extends WireMockTest {

  private static final String GAME_ID = "gameId";

  private static final LobbyGame LOBBY_GAME =
      LobbyGame.builder()
          .hostAddress("127.0.0.1")
          .hostPort(12)
          .hostName("name")
          .mapName("map")
          .playerCount(3)
          .gameRound(1)
          .epochMilliTimeStarted(Instant.now().toEpochMilli())
          .mapVersion("1")
          .passworded(false)
          .status("WAITING_FOR_PLAYERS")
          .comments("comments")
          .build();

  private static final LobbyGameListing LOBBY_GAME_LISTING =
      LobbyGameListing.builder().gameId(GAME_ID).lobbyGame(LOBBY_GAME).build();

  private static GameListingClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, GameListingClient::newClient);
  }

  @Test
  void fetchGameListing(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        get(GameListingClient.FETCH_GAMES_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(
                        HttpClientTesting.toJson(Collections.singletonList(LOBBY_GAME_LISTING)))));

    final List<LobbyGameListing> results = newClient(server).fetchGameListing();

    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(LOBBY_GAME_LISTING));
  }

  @Test
  void postGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(GameListingClient.POST_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalToJson(HttpClientTesting.toJson(LOBBY_GAME)))
            .willReturn(WireMock.aResponse().withStatus(200).withBody(GAME_ID)));

    final String gameId = newClient(server).postGame(LOBBY_GAME);

    assertThat(gameId, is(GAME_ID));
  }

  @Test
  void updateGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(GameListingClient.UPDATE_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(
                equalToJson(
                    HttpClientTesting.toJson(
                        UpdateGameRequest.builder().gameId(GAME_ID).gameData(LOBBY_GAME).build())))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).updateGame(GAME_ID, LOBBY_GAME);
  }

  @Test
  void sendKeepAlive(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(GameListingClient.KEEP_ALIVE_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(GAME_ID))
            .willReturn(WireMock.aResponse().withStatus(200).withBody("true")));

    final boolean result = newClient(server).sendKeepAlive(GAME_ID);

    assertThat(result, is(true));
  }

  @Test
  void removeGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(GameListingClient.REMOVE_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo(GAME_ID))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).removeGame(GAME_ID);
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
