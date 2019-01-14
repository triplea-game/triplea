package org.triplea.lobby.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.game.server.HeadlessGameServer;

import games.strategy.engine.lobby.server.GameDescription;

final class GameDescriptionTest {
  @Test
  void isBotShouldReturnTrueWhenCommentAndNamePrefixMatch() {
    final GameDescription gameDescription = GameDescription.builder()
        .comment(HeadlessGameServer.BOT_GAME_HOST_COMMENT)
        .hostName(HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX)
        .build();

    assertThat(gameDescription.isBot(), is(true));
  }

  @ParameterizedTest
  @MethodSource("notBotDescriptions")
  void isBotShouldReturnFalseWhen(final GameDescription shouldNotBeBot) {
    assertThat(shouldNotBeBot.toString(), shouldNotBeBot.isBot(), is(false));
  }

  private static Collection<GameDescription> notBotDescriptions() {
    return Arrays.asList(
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
            .build());
  }
}
