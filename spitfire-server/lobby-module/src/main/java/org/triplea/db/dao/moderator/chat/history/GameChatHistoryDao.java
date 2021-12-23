package org.triplea.db.dao.moderator.chat.history;

import java.util.List;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

/**
 * DAO to access history of in-game chat messages stored in database. Chat messages are stored for
 * all lobby-connected games.
 */
public interface GameChatHistoryDao {

  @SqlQuery(
      "select "
          + "    gch.date,"
          + "    gch.username,"
          + "    gch.message"
          + "  from game_chat_history gch"
          + "  join lobby_game lg on lg.id = gch.lobby_game_id"
          + "  where"
          + "    lg.game_id = :gameId"
          + "    and date > (now() - '6 hour'::interval)")
  List<ChatHistoryRecord> getChatHistory(@Bind("gameId") String gameId);
}
