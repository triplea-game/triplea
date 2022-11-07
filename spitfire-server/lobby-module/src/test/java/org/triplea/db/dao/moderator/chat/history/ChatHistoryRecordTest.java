package org.triplea.db.dao.moderator.chat.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChatHistoryRecordTest {

  @Test
  void toChatHistoryMessage() {
    final var chatHistoryRecord =
        ChatHistoryRecord.builder().date(Instant.now()).message("message").username("user").build();

    final var chatHistoryMessage = chatHistoryRecord.toChatHistoryMessage();

    assertThat(
        chatHistoryMessage.getEpochMilliDate(), is(chatHistoryRecord.getDate().toEpochMilli()));
    assertThat(chatHistoryMessage.getUsername(), is(chatHistoryRecord.getUsername()));
    assertThat(chatHistoryMessage.getMessage(), is(chatHistoryRecord.getMessage()));
  }
}
