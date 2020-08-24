package org.triplea.db.dao.chat.history;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import org.junit.jupiter.api.Test;
import org.triplea.db.dao.DaoTest;

class LobbyChatHistoryDaoTest extends DaoTest {

  private final LobbyChatHistoryDao lobbyChatHistoryDao = DaoTest.newDao(LobbyChatHistoryDao.class);

  @Test
  @DataSet(cleanBefore = true, value = "chat_history/insert_into_lobby_chat_history_before.yml")
  @ExpectedDataSet("chat_history/insert_into_lobby_chat_history_after.yml")
  void insertChatMessage() {
    lobbyChatHistoryDao.insertMessage("username", 3000, "message");
  }
}
