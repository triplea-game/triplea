package games.strategy.engine.lobby.client.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

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

      assertThat(LobbyGamePanel.getBotSupportEmail(gameDescription), is(botSupportEmail));
    }

    @Test
    public void shouldReturnEmptyStringWhenGameDescriptionIsNull() {
      assertThat(LobbyGamePanel.getBotSupportEmail(null), is(emptyString()));
    }

    @Test
    public void shouldReturnEmptyStringWhenBotSupportEmailIsNull() {
      when(gameDescription.getBotSupportEmail()).thenReturn(null);

      assertThat(LobbyGamePanel.getBotSupportEmail(gameDescription), is(emptyString()));
    }
  }
}
