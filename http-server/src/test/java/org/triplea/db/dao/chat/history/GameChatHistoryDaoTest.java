package org.triplea.db.dao.chat.history;

import com.github.database.rider.core.api.dataset.DataSet;
import org.junit.jupiter.api.Test;
import org.triplea.db.dao.DaoTest;
import org.triplea.http.client.lobby.chat.upload.ChatMessageUpload;

class GameChatHistoryDaoTest extends DaoTest {
  private final GameChatHistoryDao gameChatHistoryDao = DaoTest.newDao(GameChatHistoryDao.class);

  @Test
  @DataSet(cleanBefore = true, value = "chat_history/game_chat_history_insert_after.yml")
  void insertChatMessage() {
    gameChatHistoryDao.recordChat(
        ChatMessageUpload.builder()
            .hostName("hostname")
            .gameId("gameid-100")
            .fromPlayer("gameplayer")
            .chatMessage("example message")
            .build());
  }
}
