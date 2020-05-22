package org.triplea.http.client.lobby.player;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.triplea.http.client.HttpClientTesting.EXPECTED_API_KEY;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.WireMockTest;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import ru.lanwen.wiremock.ext.WiremockResolver;

class PlayerLobbyActionsClientTest extends WireMockTest {

  private static final PlayerSummary PLAYER_SUMMARY_FOR_MODERATOR =
      PlayerSummary.builder()
          .systemId("system-id")
          .ip("5.5.3.3")
          .aliases(List.of())
          .bans(
              List.of(
                  PlayerSummary.BanInformation.builder()
                      .epochMillEndDate(1000)
                      .epochMilliStartDate(2000)
                      .name("name-banned")
                      .systemId("id")
                      .ip("ip")
                      .build()))
          .build();

  private static final PlayerSummary PLAYER_SUMMARY_FOR_PLAYER = PlayerSummary.builder().build();

  private static PlayerLobbyActionsClient newClient(final WireMockServer wireMockServer) {
    return newClient(wireMockServer, PlayerLobbyActionsClient::new);
  }

  @Test
  void fetchPlayerInfoAsModerator(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(PlayerLobbyActionsClient.FETCH_PLAYER_INFORMATION)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo("player-chat-id"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(toJson(PLAYER_SUMMARY_FOR_MODERATOR))));

    final var result = newClient(server).fetchPlayerInformation(PlayerChatId.of("player-chat-id"));
    assertThat(result, is(PLAYER_SUMMARY_FOR_MODERATOR));
  }

  @Test
  void fetchPlayerInfoAsPlayer(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(PlayerLobbyActionsClient.FETCH_PLAYER_INFORMATION)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo("player-chat-id"))
            .willReturn(
                WireMock.aResponse().withStatus(200).withBody(toJson(PLAYER_SUMMARY_FOR_PLAYER))));

    final var result = newClient(server).fetchPlayerInformation(PlayerChatId.of("player-chat-id"));
    assertThat(result, is(PLAYER_SUMMARY_FOR_PLAYER));
  }

  @Test
  void fetchPlayersInGame(@WiremockResolver.Wiremock final WireMockServer server) {
    server.stubFor(
        WireMock.post(PlayerLobbyActionsClient.FETCH_PLAYERS_IN_GAME)
            .withHeader(AuthenticationHeaders.API_KEY_HEADER, equalTo(EXPECTED_API_KEY))
            .withRequestBody(equalTo("game-id"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withBody(toJson(List.of("player1", "player2")))));

    final var result = newClient(server).fetchPlayersInGame("game-id");
    assertThat(result, hasSize(2));
    assertThat(result, hasItems("player1", "player2"));
  }
}
