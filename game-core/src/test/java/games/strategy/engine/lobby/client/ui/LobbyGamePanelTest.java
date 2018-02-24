package games.strategy.engine.lobby.client.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;

import com.example.mockito.MockitoExtension;

import games.strategy.engine.lobby.server.GameDescription;

public final class LobbyGamePanelTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class GetBotSupportEmailTest {
    @Spy
    private final GameDescription gameDescription = new GameDescription();

    @Test
    public void shouldReturnBotSupportEmailWhenBotSupportEmailIsNotNull() {
      final String botSupportEmail = "bot@me.com";
      when(gameDescription.getBotSupportEmail()).thenReturn(botSupportEmail);

      assertThat(LobbyGamePanel.getBotSupportEmail(gameDescription), is(Optional.of(botSupportEmail)));
    }

    @Test
    public void shouldReturnEmptyWhenGameDescriptionIsNull() {
      assertThat(LobbyGamePanel.getBotSupportEmail(null), is(Optional.empty()));
    }

    @Test
    public void shouldReturnEmptyWhenBotSupportEmailIsNull() {
      when(gameDescription.getBotSupportEmail()).thenReturn(null);

      assertThat(LobbyGamePanel.getBotSupportEmail(gameDescription), is(Optional.empty()));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class IsBotTest {
    @Spy
    private final GameDescription gameDescription = new GameDescription();

    @Test
    public void shouldReturnTrueWhenBotSupportEmailIsNotNull() {
      when(gameDescription.getBotSupportEmail()).thenReturn("bot@me.com");

      assertThat(LobbyGamePanel.isBot(gameDescription), is(true));
    }

    @Test
    public void shouldReturnFalseWhenGameDescriptionIsNull() {
      assertThat(LobbyGamePanel.isBot(null), is(false));
    }

    @Test
    public void shouldReturnFalseWhenBotSupportEmailIsNull() {
      when(gameDescription.getBotSupportEmail()).thenReturn(null);

      assertThat(LobbyGamePanel.isBot(gameDescription), is(false));
    }
  }
}
