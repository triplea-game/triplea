package org.triplea.db.dao.lobby.games;

import com.google.common.base.Ascii;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatMessageUpload;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.java.Postconditions;

/**
 * Game chat history table stores chat messages that have happened in games. This data is upload by
 * game servers to the lobby and is then recorded in database.
 */
public interface LobbyGameDao {
  int MESSAGE_COLUMN_LENGTH = 240;

  default void insertLobbyGame(ApiKey apiKey, LobbyGameListing lobbyGameListing) {
    final int insertCount =
        insertLobbyGame(
            lobbyGameListing.getLobbyGame().getHostName(),
            lobbyGameListing.getGameId(),
            apiKey.getValue());
    Postconditions.assertState(
        insertCount == 1, "Failed to insert lobby game: " + lobbyGameListing);
  }

  @SqlUpdate(
      "insert into lobby_game(host_name, game_id, game_hosting_api_key_id) "
          + "values ("
          + "  :hostName,"
          + "  :gameId,"
          + "  (select id from game_hosting_api_key where key = :apiKey))")
  int insertLobbyGame(
      @Bind("hostName") String hostName,
      @Bind("gameId") String gameId,
      @Bind("apiKey") String apiKey);

  default void recordChat(final ChatMessageUpload chatMessageUpload) {
    final int rowInsert =
        insertChatMessage(
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
