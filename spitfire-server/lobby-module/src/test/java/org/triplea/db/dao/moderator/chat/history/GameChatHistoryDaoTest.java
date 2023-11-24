package org.triplea.db.dao.moderator.chat.history;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.triplea.test.common.IsInstant.isInstant;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.test.common.RequiresDatabase;

@DataSet(
    value =
        "game_chat_history/game_hosting_api_key.yml,"
            + "game_chat_history/lobby_game.yml,"
            + "game_chat_history/game_chat_history.yml",
    useSequenceFiltering = false)
@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class GameChatHistoryDaoTest {
  private static final String SIR_HOSTS_A_LOT = "sir_hosts_a_lot";
  private static final String SIR_HOSTS_A_LITTLE = "sir_hosts_a_little";
  private static final String PLAYER1 = "player1";

  private final GameChatHistoryDao gameChatHistoryDao;

  @Test
  void gameDoesNotExist() {
    assertThat(gameChatHistoryDao.getChatHistory("dne"), is(empty()));
  }

  @Test
  @DisplayName("Chat history for a game with no chat messages should be empty")
  void emptyChatHistory() {
    assertThat(gameChatHistoryDao.getChatHistory("game-empty-chat"), is(empty()));
  }

  @Test
  @DisplayName("Chat history does not select all chat messages, only recent past")
  void oldChatMessagesAreFiltered() {
    assertThat(gameChatHistoryDao.getChatHistory("game-far-past"), is(empty()));
  }

  @Test
  void viewChatHistoryForSirHostALotsGame() {
    final List<ChatHistoryRecord> chats = gameChatHistoryDao.getChatHistory("game-hosts-a-lot");

    assertThat(chats, IsCollectionWithSize.hasSize(4));

    int i = 0;
    assertThat(chats.get(i).getUsername(), is(PLAYER1));
    assertThat(chats.get(i).getDate(), isInstant(2100, 1, 1, 23, 0, 20));
    assertThat(chats.get(i).getMessage(), is("Hello good sir"));

    i++;
    assertThat(chats.get(i).getUsername(), is(SIR_HOSTS_A_LOT));
    assertThat(chats.get(i).getDate(), isInstant(2100, 1, 1, 23, 1, 20));
    assertThat(chats.get(i).getMessage(), is("Why hello to you"));

    i++;
    assertThat(chats.get(i).getUsername(), is(PLAYER1));
    assertThat(chats.get(i).getDate(), isInstant(2100, 1, 1, 23, 2, 20));
    assertThat(chats.get(i).getMessage(), is("What a fine day it is my good sir"));

    i++;
    assertThat(chats.get(i).getUsername(), is(SIR_HOSTS_A_LOT));
    assertThat(chats.get(i).getDate(), isInstant(2100, 1, 1, 23, 3, 20));
    assertThat(chats.get(i).getMessage(), is("What a fine day it is indeed!"));
  }

  @Test
  void viewChatHistoryForSirHostALittlesGame() {
    final List<ChatHistoryRecord> chats = gameChatHistoryDao.getChatHistory("game-hosts-a-little");

    assertThat(chats, IsCollectionWithSize.hasSize(3));

    int i = 0;
    assertThat(chats.get(i).getUsername(), is(SIR_HOSTS_A_LITTLE));
    assertThat(chats.get(i).getDate(), isInstant(2100, 1, 1, 23, 1, 20));
    assertThat(chats.get(i).getMessage(), is("hello!"));

    i++;
    assertThat(chats.get(i).getUsername(), is(SIR_HOSTS_A_LOT));
    assertThat(chats.get(i).getDate(), isInstant(2100, 1, 1, 23, 2, 20));
    assertThat(chats.get(i).getMessage(), is("join my game?"));

    i++;
    assertThat(chats.get(i).getUsername(), is(SIR_HOSTS_A_LITTLE));
    assertThat(chats.get(i).getDate(), isInstant(2100, 1, 1, 23, 3, 20));
    assertThat(chats.get(i).getMessage(), is("Maybe another day"));
  }
}
