package org.triplea.lobby.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import games.strategy.net.Node;
import java.net.InetAddress;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.http.client.lobby.game.listing.LobbyGame;

final class GameDescriptionTest {
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
          .status(GameDescription.GameStatus.WAITING_FOR_PLAYERS.toString())
          .comments("comments")
          .build();

  @Nested
  final class IsBotTest {
    @Test
    void isBotShouldReturnTrueWhenCommentAndNamePrefixMatch() throws Exception {
      final GameDescription gameDescription =
          GameDescription.builder()
              .hostedBy(
                  new Node(
                      HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX, InetAddress.getLocalHost(), 10))
              .comment(HeadlessGameServer.BOT_GAME_HOST_COMMENT)
              .build();

      assertThat(gameDescription.isBot(), is(true));
    }

    @Test
    void isBotShouldReturnFalseWhen() throws Exception {
      Arrays.asList(
              // host name must have correct prefix
              GameDescription.builder()
                  .comment(HeadlessGameServer.BOT_GAME_HOST_COMMENT)
                  .hostedBy(
                      new Node(
                          "mangling-the-prefix-" + HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX,
                          InetAddress.getLocalHost(),
                          10))
                  .build(),
              // must have the right comment
              GameDescription.builder()
                  .comment(
                      HeadlessGameServer.BOT_GAME_HOST_COMMENT
                          + HeadlessGameServer.BOT_GAME_HOST_COMMENT)
                  .hostedBy(
                      new Node(
                          "mangling-the-prefix-" + HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX,
                          InetAddress.getLocalHost(),
                          10))
                  .build())
          .forEach(
              shouldNotBeBot ->
                  assertThat(shouldNotBeBot.toString(), shouldNotBeBot.isBot(), is(false)));
    }
  }

  @Nested
  final class FormatBotStartTimeTest {
    @Test
    void nullStartDateTimeIsFormattedToEmptyString() {
      final GameDescription gameDescription = GameDescription.builder().build();

      assertThat(gameDescription.getFormattedBotStartTime(), is(""));
    }

    @Test
    void shouldNotThrowExceptionWhenStartedDateIsNotNull() {
      final GameDescription gameDescription =
          GameDescription.builder().startDateTime(Instant.now()).build();

      assertDoesNotThrow(gameDescription::getFormattedBotStartTime);
    }
  }

  @Nested
  final class ToAndFromLobbyGame {

    @Test
    void fromAndToLobbyGame() {
      final GameDescription description = GameDescription.fromLobbyGame(LOBBY_GAME);
      final LobbyGame result = description.toLobbyGame();
      assertThat(result, is(LOBBY_GAME));
    }
  }
}
