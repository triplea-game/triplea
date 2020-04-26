package org.triplea.db.dao.chat.history;

import com.google.common.base.Ascii;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.http.client.lobby.chat.upload.ChatMessageUpload;

/**
 * Game chat history table stores chat messages that have happened in games. This data is upload by
 * game servers to the lobby and is then recorded in database.
 */
public interface GameChatHistoryDao {
  int MESSAGE_COLUMN_LENGTH = 240;

  default void recordChat(final ChatMessageUpload chatMessageUpload) {
    insertChatMessage(
        chatMessageUpload.getHostName(),
        chatMessageUpload.getGameId(),
        chatMessageUpload.getFromPlayer(),
        Ascii.truncate(chatMessageUpload.getChatMessage(), MESSAGE_COLUMN_LENGTH, ""));
  }

  @SqlUpdate(
      "insert into game_chat_history (host_name, game_id, username, message) "
          + "values(:hostname, :gameId, :username, :message)")
  void insertChatMessage(
      @Bind("hostname") String hostname,
      @Bind("gameId") String gameId,
      @Bind("username") String username,
      @Bind("message") String message);
}
