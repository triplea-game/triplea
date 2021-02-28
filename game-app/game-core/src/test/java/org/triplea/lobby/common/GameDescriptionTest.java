package org.triplea.lobby.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.net.Node;
import java.net.InetAddress;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.LobbyGame;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.test.TestData;

final class GameDescriptionTest {
  private static final LobbyGame LOBBY_GAME = TestData.LOBBY_GAME;

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
      List.of(
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
  final class ToAndFromLobbyGame {

    @Test
    void fromAndToLobbyGame() {
      final GameDescription description = GameDescription.fromLobbyGame(LOBBY_GAME);
      final LobbyGame result = description.toLobbyGame();
      assertThat(result, is(LOBBY_GAME));
    }
  }
}
