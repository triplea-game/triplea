package org.triplea.db.dao.moderator.chat.history;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.http.client.lobby.moderator.ChatHistoryMessage;

/** Represents most of a row of the game_chat_history table, who said what and when. */
@Getter(onMethod_ = @VisibleForTesting)
public class ChatHistoryRecord {
  private final Instant date;
  private final String username;
  private final String message;

  @Builder
  public ChatHistoryRecord(
      @ColumnName("date") final Instant date,
      @ColumnName("username") final String username,
      @ColumnName("message") final String message) {
    this.date = date;
    this.username = username;
    this.message = message;
  }

  public ChatHistoryMessage toChatHistoryMessage() {
    return ChatHistoryMessage.builder()
        .epochMilliDate(date.toEpochMilli())
        .username(username)
        .message(message)
        .build();
  }
}
