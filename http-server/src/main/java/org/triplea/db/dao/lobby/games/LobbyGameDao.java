package org.triplea.db.dao.lobby.games;

import com.google.common.base.Ascii;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatMessageUpload;
import org.triplea.java.Postconditions;

/**
 * Game chat history table stores chat messages that have happened in games. This data is upload by
 * game servers to the lobby and is then recorded in database.
 */
public interface LobbyGameDao {
  int MESSAGE_COLUMN_LENGTH = 240;

  default void recordChat(final ChatMessageUpload chatMessageUpload) {
    int rowInsert = insertChatMessage(
        chatMessageUpload.getGameId(),
        chatMessageUpload.getFromPlayer(),
        Ascii.truncate(chatMessageUpload.getChatMessage(), MESSAGE_COLUMN_LENGTH, ""));
    Postconditions.assertState(rowInsert == 1, "Failed to insert message: " + chatMessageUpload);
  }

  @SqlUpdate(
      "insert into game_chat_history (lobby_game_id, username, message) "
          + "values("
          + " (select id from lobby_game where game_id = :gameId),"
          + ":username, "
          + ":message)")
  int insertChatMessage(
      @Bind("gameId") String gameId,
      @Bind("username") String username,
      @Bind("message") String message);
}
