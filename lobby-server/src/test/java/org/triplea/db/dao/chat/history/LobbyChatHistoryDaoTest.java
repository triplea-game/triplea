package org.triplea.db.dao.chat.history;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.triplea.modules.http.LobbyServerTest;

@RequiredArgsConstructor
class LobbyChatHistoryDaoTest extends LobbyServerTest {

  private final LobbyChatHistoryDao lobbyChatHistoryDao;

  @Test
  @DataSet(cleanBefore = true, value = "chat_history/insert_into_lobby_chat_history_before.yml")
  @ExpectedDataSet("chat_history/insert_into_lobby_chat_history_after.yml")
  void insertChatMessage() {
    lobbyChatHistoryDao.insertMessage("username", 3000, "message");
  }
}
