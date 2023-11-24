package org.triplea.db.dao.lobby.games;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.TestData;
import org.triplea.db.LobbyModuleDatabaseTestSupport;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatMessageUpload;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.test.common.RequiresDatabase;

@RequiredArgsConstructor
@ExtendWith(LobbyModuleDatabaseTestSupport.class)
@ExtendWith(DBUnitExtension.class)
@RequiresDatabase
class LobbyGameDaoTest {
  private final LobbyGameDao lobbyGameDao;

  @Test
  @DataSet(value = "lobby_games/game_hosting_api_key.yml", useSequenceFiltering = false)
  @ExpectedDataSet("lobby_games/lobby_game_post_insert.yml")
  void insertLobbyGame() {
    lobbyGameDao.insertLobbyGame(
        ApiKey.of("HOST"),
        LobbyGameListing.builder() //
            .gameId("game-id")
            .lobbyGame(TestData.LOBBY_GAME)
            .build());
  }

  @Test
  @DataSet(
      value = "lobby_games/game_hosting_api_key.yml, lobby_games/lobby_game.yml",
      useSequenceFiltering = false)
  @ExpectedDataSet("lobby_games/game_chat_history_post_insert.yml")
  void insertChatMessage() {
    lobbyGameDao.recordChat(
        ChatMessageUpload.builder()
            .gameId("gameid-100")
            .fromPlayer("gameplayer")
            .chatMessage("example message")
            .build());
  }
}
