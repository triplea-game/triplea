package org.triplea.modules.moderation.chat.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.moderator.chat.history.ChatHistoryRecord;
import org.triplea.db.dao.moderator.chat.history.GameChatHistoryDao;
import org.triplea.http.client.lobby.moderator.ChatHistoryMessage;

@ExtendWith(MockitoExtension.class)
class FetchGameChatHistoryModuleTest {

  private static final ChatHistoryRecord CHAT_HISTORY_RECORD_0 =
      ChatHistoryRecord.builder() //
          .username("user")
          .message("message")
          .date(Instant.now())
          .build();

  private static final ChatHistoryRecord CHAT_HISTORY_RECORD_1 =
      ChatHistoryRecord.builder() //
          .username("user2")
          .message(" ")
          .date(Instant.EPOCH)
          .build();

  @Mock private GameChatHistoryDao gameChatHistoryDao;

  @InjectMocks private FetchGameChatHistoryModule fetchGameChatHistoryModule;

  @Test
  void emptyCase() {
    when(gameChatHistoryDao.getChatHistory("game-id")).thenReturn(List.of());

    assertThat(fetchGameChatHistoryModule.apply("game-id"), is(empty()));
  }

  @Test
  void convertsChatRecordsToChatHistoryMessagesInOrder() {
    when(gameChatHistoryDao.getChatHistory("game-id"))
        .thenReturn(List.of(CHAT_HISTORY_RECORD_0, CHAT_HISTORY_RECORD_1));

    final List<ChatHistoryMessage> results = fetchGameChatHistoryModule.apply("game-id");

    assertThat(results, hasSize(2));

    assertThat(results.get(0), is(CHAT_HISTORY_RECORD_0.toChatHistoryMessage()));
    assertThat(results.get(1), is(CHAT_HISTORY_RECORD_1.toChatHistoryMessage()));
  }
}
