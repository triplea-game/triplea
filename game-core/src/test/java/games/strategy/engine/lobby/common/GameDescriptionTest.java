package games.strategy.engine.lobby.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.GameDescription;

final class GameDescriptionTest {
  @Nested
  final class GetBotSupportEmailTest {
    @Test
    void shouldReturnBotSupportEmailWhenBotSupportEmailIsNotEmpty() {
      final String botSupportEmail = "bot@me.com";
      final GameDescription gameDescription = GameDescription.builder().botSupportEmail(botSupportEmail).build();

      assertThat(gameDescription.getBotSupportEmail(), is(Optional.of(botSupportEmail)));
    }

    @Test
    void shouldReturnEmptyWhenBotSupportEmailIsEmpty() {
      final GameDescription gameDescription = GameDescription.builder().botSupportEmail("").build();

      assertThat(gameDescription.getBotSupportEmail(), is(Optional.empty()));
    }
  }

  @Nested
  final class IsBotTest {
    @Test
    void shouldReturnTrueWhenBotSupportEmailIsNotEmpty() {
      final GameDescription gameDescription = GameDescription.builder().botSupportEmail("bot@me.com").build();

      assertThat(gameDescription.isBot(), is(true));
    }

    @Test
    void shouldReturnFalseWhenBotSupportEmailIsEmpty() {
      final GameDescription gameDescription = GameDescription.builder().botSupportEmail("").build();

      assertThat(gameDescription.isBot(), is(false));
    }
  }
}
