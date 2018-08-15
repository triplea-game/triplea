package games.strategy.engine.lobby.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.GameDescription;

public final class GameDescriptionTest {
  @Nested
  public final class GetBotSupportEmailTest {
    private final GameDescription gameDescription = new GameDescription();

    @Test
    public void shouldReturnBotSupportEmailWhenBotSupportEmailIsNotEmpty() {
      final String botSupportEmail = "bot@me.com";
      gameDescription.setBotSupportEmail(botSupportEmail);

      assertThat(gameDescription.getBotSupportEmail(), is(Optional.of(botSupportEmail)));
    }

    @Test
    public void shouldReturnEmptyWhenBotSupportEmailIsEmpty() {
      gameDescription.setBotSupportEmail("");

      assertThat(gameDescription.getBotSupportEmail(), is(Optional.empty()));
    }
  }

  @Nested
  public final class IsBotTest {
    private final GameDescription gameDescription = new GameDescription();

    @Test
    public void shouldReturnTrueWhenBotSupportEmailIsNotEmpty() {
      gameDescription.setBotSupportEmail("bot@me.com");

      assertThat(gameDescription.isBot(), is(true));
    }

    @Test
    public void shouldReturnFalseWhenBotSupportEmailIsEmpty() {
      gameDescription.setBotSupportEmail("");

      assertThat(gameDescription.isBot(), is(false));
    }
  }
}
