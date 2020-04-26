package org.triplea.db.dao.lobby.games;

import com.github.database.rider.core.api.dataset.DataSet;
import org.junit.jupiter.api.Test;
import org.triplea.db.dao.DaoTest;
import org.triplea.db.dao.lobby.games.LobbyGameDao;
import org.triplea.http.client.lobby.chat.upload.ChatMessageUpload;

class LobbyGameDaoTest extends DaoTest {
  private final LobbyGameDao lobbyGameDao = DaoTest.newDao(LobbyGameDao.class);

  @Test
  @DataSet(cleanBefore = true, value = "chat_history/game_chat_history_insert_after.yml")
  void insertChatMessage() {
    lobbyGameDao.recordChat(
        ChatMessageUpload.builder()
            .hostName("hostname")
            .gameId("gameid-100")
            .fromPlayer("gameplayer")
            .chatMessage("example message")
            .build());
  }
}
