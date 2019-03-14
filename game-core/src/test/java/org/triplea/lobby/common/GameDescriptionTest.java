package org.triplea.lobby.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.game.server.HeadlessGameServer;

final class GameDescriptionTest {
  @Nested
  final class IsBotTest {
    @Test
    void isBotShouldReturnTrueWhenCommentAndNamePrefixMatch() {
      final GameDescription gameDescription = GameDescription.builder()
          .comment(HeadlessGameServer.BOT_GAME_HOST_COMMENT)
          .hostName(HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX)
          .build();

      assertThat(gameDescription.isBot(), is(true));
    }

    @Test
    void isBotShouldReturnFalseWhen() {
      Arrays.asList(
          // host name must have correct prefix
          GameDescription.builder()
              .comment(HeadlessGameServer.BOT_GAME_HOST_COMMENT)
              .hostName("")
              .build(),
          // host name must have correct prefix
          GameDescription.builder()
              .comment(HeadlessGameServer.BOT_GAME_HOST_COMMENT)
              .hostName("mangling-the-prefix-" + HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX)
              .build(),
          // must have the right comment
          GameDescription.builder()
              .comment(HeadlessGameServer.BOT_GAME_HOST_COMMENT + HeadlessGameServer.BOT_GAME_HOST_COMMENT)
              .hostName("mangling-the-prefix-" + HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX)
              .build())
          .forEach(
              shouldNotBeBot -> assertThat(shouldNotBeBot.toString(), shouldNotBeBot.isBot(), is(false)));
    }
  }
}
