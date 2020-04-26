package org.triplea.modules.chat.upload;

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
import org.triplea.db.dao.chat.history.GameChatHistoryDao;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.chat.upload.ChatMessageUpload;

@ExtendWith(MockitoExtension.class)
class ChatUploadModuleTest {
  private static final ChatMessageUpload CHAT_MESSAGE_UPLOAD =
      ChatMessageUpload.builder()
          .hostName("hostname")
          .gameId("game-id")
          .apiKey("api-key")
          .chatMessage("message")
          .fromPlayer("player")
          .build();

  @Mock private GameChatHistoryDao gameHostChatHistoryDao;
  @Mock private BiPredicate<ApiKey, String> gameIdValidator;

  @InjectMocks private ChatUploadModule chatUploadModule;

  @Test
  void validApiAndGameIdPair() {
    givenApiKeyIsValid(true);

    chatUploadModule.accept(CHAT_MESSAGE_UPLOAD);

    verify(gameHostChatHistoryDao).recordChat(CHAT_MESSAGE_UPLOAD);
  }

  @Test
  void inValidApiAndGameIdPair() {
    givenApiKeyIsValid(false);

    chatUploadModule.accept(CHAT_MESSAGE_UPLOAD);

    verify(gameHostChatHistoryDao, never()).recordChat(any());
  }

  private void givenApiKeyIsValid(final boolean isValid) {
    when(gameIdValidator.test(
            ApiKey.of(CHAT_MESSAGE_UPLOAD.getApiKey()), CHAT_MESSAGE_UPLOAD.getGameId()))
        .thenReturn(isValid);
  }
}
