package org.triplea.db.dao.lobby.games;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatMessageUpload;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.modules.TestData;
import org.triplea.modules.http.LobbyServerTest;

@RequiredArgsConstructor
class LobbyGameDaoTest extends LobbyServerTest {
  private final LobbyGameDao lobbyGameDao;

  @Test
  @DataSet(cleanBefore = true, value = "lobby_games/lobby_game_insert_before.yml")
  @ExpectedDataSet("lobby_games/lobby_game_insert_after.yml")
  void insertLobbyGame() {
    lobbyGameDao.insertLobbyGame(
        ApiKey.of("HOST"),
        LobbyGameListing.builder() //
            .gameId("game-id")
            .lobbyGame(TestData.LOBBY_GAME)
            .build());
  }

  @Test
  @DataSet(cleanBefore = true, value = "lobby_games/game_chat_history_insert_before.yml")
  @ExpectedDataSet("lobby_games/game_chat_history_insert_after.yml")
  void insertChatMessage() {
    lobbyGameDao.recordChat(
        ChatMessageUpload.builder()
            .gameId("gameid-100")
            .fromPlayer("gameplayer")
            .chatMessage("example message")
            .build());
  }
}
