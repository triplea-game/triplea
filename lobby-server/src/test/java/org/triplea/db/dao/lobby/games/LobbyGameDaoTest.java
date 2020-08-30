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
