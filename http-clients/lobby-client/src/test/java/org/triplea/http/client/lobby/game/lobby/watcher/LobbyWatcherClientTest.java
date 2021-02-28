package org.triplea.http.client.lobby.game.lobby.watcher;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.TestData;
import org.triplea.http.client.WireMockTest;
import org.triplea.test.common.JsonUtil;
import ru.lanwen.wiremock.ext.WiremockResolver;

class LobbyWatcherClientTest extends WireMockTest {

  private static final String GAME_ID = "gameId";

  private static final GamePostingRequest GAME_POSTING_REQUEST =
      GamePostingRequest.builder().playerNames(List.of()).lobbyGame(TestData.LOBBY_GAME).build();

  private static LobbyWatcherClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, LobbyWatcherClient::newClient);
  }

  @Test
  void postGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(LobbyWatcherClient.POST_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalToJson(JsonUtil.toJson(GAME_POSTING_REQUEST)))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(
                        JsonUtil.toJson(
                            GamePostingResponse.builder()
                                .gameId(GAME_ID)
                                .connectivityCheckSucceeded(true)
                                .build()))));

    final GamePostingResponse response = newClient(server).postGame(GAME_POSTING_REQUEST);

    assertThat(response.getGameId(), is(GAME_ID));
    assertThat(response.isConnectivityCheckSucceeded(), is(true));
  }

  @Test
  void updateGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        post(LobbyWatcherClient.UPDATE_GAME_PATH)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(
                equalToJson(
                    JsonUtil.toJson(
                        UpdateGameRequest.builder()
                            .gameId(GAME_ID)
                            .gameData(TestData.LOBBY_GAME)
                            .build())))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(server).updateGame(GAME_ID, TestData.LOBBY_GAME);
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

  @Test
  void sendGameHostingRequest(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        post(LobbyWatcherClient.UPLOAD_CHAT_PATH).willReturn(WireMock.aResponse().withStatus(200)));

    newClient(wireMockServer)
        .uploadChatMessage(
            ApiKey.newKey(),
            ChatUploadParams.builder()
                .gameId("game-id")
                .chatMessage("chat-message")
                .fromPlayer(UserName.of("player"))
                .build());
  }

  @Test
  void sendPlayerJoinedNotification(
      @WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        post(LobbyWatcherClient.PLAYER_JOINED_PATH)
            .withRequestBody(
                equalToJson(
                    JsonUtil.toJson(
                        PlayerJoinedNotification.builder()
                            .gameId("game-id")
                            .playerName("player-joined")
                            .build())))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(wireMockServer).playerJoined("game-id", UserName.of("player-joined"));
  }

  @Test
  void sendPlayerLeftNotification(@WiremockResolver.Wiremock final WireMockServer wireMockServer) {
    wireMockServer.stubFor(
        post(LobbyWatcherClient.PLAYER_LEFT_PATH)
            .withRequestBody(
                equalToJson(
                    JsonUtil.toJson(
                        PlayerLeftNotification.builder()
                            .gameId("game-id")
                            .playerName("player-left")
                            .build())))
            .willReturn(WireMock.aResponse().withStatus(200)));

    newClient(wireMockServer).playerLeft("game-id", UserName.of("player-left"));
  }
}
