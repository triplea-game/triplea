package org.triplea.db.dao.moderator.chat.history;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.TimestampMapper;
import org.triplea.http.client.lobby.moderator.ChatHistoryMessage;

/** Represents most of a row of the game_chat_history table, who said what and when. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatHistoryRecord {
  private Instant date;
  private String username;
  private String message;

  /** Returns a JDBI row mapper used to convert results into an instance of this bean object. */
  public static RowMapper<ChatHistoryRecord> buildResultMapper() {
    return (rs, ctx) ->
        ChatHistoryRecord.builder()
            .date(TimestampMapper.map(rs, "date"))
            .username(rs.getString("username"))
            .message(rs.getString("message"))
            .build();
  }

  public ChatHistoryMessage toChatHistoryMessage() {
    return ChatHistoryMessage.builder()
        .epochMilliDate(date.toEpochMilli())
        .username(username)
        .message(message)
        .build();
  }
}
