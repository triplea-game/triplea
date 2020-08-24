package org.triplea.modules.game.lobby.watcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.lobby.games.LobbyGameDao;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatMessageUpload;

@ExtendWith(MockitoExtension.class)
class ChatUploadModuleTest {
  private static final ChatMessageUpload CHAT_MESSAGE_UPLOAD =
      ChatMessageUpload.builder()
          .gameId("game-id")
          .apiKey("api-key")
          .chatMessage("message")
          .fromPlayer("player")
          .build();

  @Mock private LobbyGameDao lobbyGameDao;
  @Mock private BiPredicate<ApiKey, String> gameIdValidator;

  @InjectMocks private ChatUploadModule chatUploadModule;

  @Test
  void validApiAndGameIdPair() {
    givenApiKeyIsValid(true);

    final boolean result = chatUploadModule.upload(CHAT_MESSAGE_UPLOAD);

    assertThat(result, is(true));
    verify(lobbyGameDao).recordChat(CHAT_MESSAGE_UPLOAD);
  }

  @Test
  void inValidApiAndGameIdPair() {
    givenApiKeyIsValid(false);

    final boolean result = chatUploadModule.upload(CHAT_MESSAGE_UPLOAD);

    assertThat(result, is(false));
    verify(lobbyGameDao, never()).recordChat(any());
  }

  private void givenApiKeyIsValid(final boolean isValid) {
    when(gameIdValidator.test(
            ApiKey.of(CHAT_MESSAGE_UPLOAD.getApiKey()), CHAT_MESSAGE_UPLOAD.getGameId()))
        .thenReturn(isValid);
  }
}
