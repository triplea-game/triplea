package org.triplea.server.lobby.game.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.server.lobby.game.listing.GameListing.GameId;

/**
 * Items to test.: <br>
 * - core functionality of get, add, remove, and keep-alive<br>
 * - games are added with an API key, keep-alive and remove should not do anything if API key is
 * incorrect<br>
 * - 'dead-games' should be reaped on 'get'<br>
 * - boot-game, the authentication should be already done on controller level, so we just need to be
 * sure there is bookkeeping.
 */
@ExtendWith(MockitoExtension.class)
class GameListingTest {
  private static final String GAME_ID_0 = "id0";
  private static final String GAME_ID_1 = "id1";
  private static final String GAME_ID_2 = "id2";

  private static final ApiKey API_KEY_0 = ApiKey.of("apiKey0");
  private static final ApiKey API_KEY_1 = ApiKey.of("apiKey1");

  private static final GameId ID_0 = new GameId(API_KEY_0, GAME_ID_0);

  private static final String HOST_NAME = "host-player";
  private static final int MODERATOR_ID = 33;

  @Mock private ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  @Mock private Cache<GameId, LobbyGame> cache;
  @Mock private GameListingEventQueue gameListingEventQueue;

  private GameListing gameListing;

  @Mock private LobbyGame lobbyGame0;
  @Mock private LobbyGame lobbyGame1;
  @Mock private LobbyGame lobbyGame2;

  @BeforeEach
  void setup() {
    gameListing =
        GameListing.builder()
            .gameListingEventQueue(gameListingEventQueue)
            .auditHistoryDao(moderatorAuditHistoryDao)
            .games(cache)
            .build();
  }

  @Nested
  final class GetGames {

    /** Basic case, no games added, expect none to be returned. */
    @Test
    void getGames() {
      final Map<GameId, LobbyGame> games = new HashMap<>();
      games.put(ID_0, lobbyGame0);
      games.put(new GameId(API_KEY_0, GAME_ID_1), lobbyGame1);
      games.put(new GameId(API_KEY_1, GAME_ID_2), lobbyGame2);

      when(cache.asMap()).thenReturn(new ConcurrentHashMap<>(games));

      final List<LobbyGameListing> result = gameListing.getGames();

      assertThat(result.size(), is(games.size()));
      assertThat(
          result,
          IsCollectionContaining.hasItem(
              LobbyGameListing.builder().gameId(GAME_ID_0).lobbyGame(lobbyGame0).build()));

      assertThat(
          result,
          IsCollectionContaining.hasItem(
              LobbyGameListing.builder().gameId(GAME_ID_1).lobbyGame(lobbyGame1).build()));

      assertThat(
          result,
          IsCollectionContaining.hasItem(
              LobbyGameListing.builder().gameId(GAME_ID_2).lobbyGame(lobbyGame2).build()));
    }
  }

  @Nested
  final class KeepAlive {
    @Test
    void noGamesPresent() {
      when(cache.getIfPresent(ID_0)).thenReturn(null);

      final boolean result = gameListing.keepAlive(API_KEY_0, GAME_ID_0);
      assertThat("Game not found, keep alive should return false", result, is(false));

      verify(cache, never()).put(any(), any());
    }

    @Test
    void gameExists() {
      when(cache.getIfPresent(ID_0)).thenReturn(lobbyGame0);

      final boolean result = gameListing.keepAlive(API_KEY_0, GAME_ID_0);

      assertThat("Game found, keep alive should return true", result, is(true));
      verify(cache).put(ID_0, lobbyGame0);
    }
  }

  @Nested
  final class RemoveGame {
    @Test
    void removeGame() {
      final ConcurrentMap<GameId, LobbyGame> map = new ConcurrentHashMap<>();
      map.put(new GameId(API_KEY_0, GAME_ID_0), lobbyGame0);
      when(cache.asMap()).thenReturn(map);

      gameListing.removeGame(API_KEY_0, GAME_ID_0);

      verify(cache).invalidate(ID_0);
      verify(gameListingEventQueue).gameRemoved(GAME_ID_0);
    }

    @Test
    void removeGameRequiresCorrectApiKey() {
      final ConcurrentMap<GameId, LobbyGame> map = new ConcurrentHashMap<>();
      map.put(new GameId(API_KEY_0, GAME_ID_0), lobbyGame0);
      when(cache.asMap()).thenReturn(map);

      gameListing.removeGame(API_KEY_1, GAME_ID_0);

      verify(cache, never()).invalidate(any(GameId.class));
      verify(gameListingEventQueue, never()).gameRemoved(any());
    }
  }

  @Nested
  final class PostGame {
    @Test
    void postGame() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      assertThat(id0, not(emptyString()));
      verify(cache).put(new GameId(API_KEY_0, id0), lobbyGame0);
      verify(gameListingEventQueue)
          .gameUpdated(LobbyGameListing.builder().gameId(id0).lobbyGame(lobbyGame0).build());
    }
  }

  @Nested
  final class UpdateGame {
    @Test
    void updateGameThatDoesNotExist() {
      when(cache.asMap()).thenReturn(new ConcurrentHashMap<>());

      final boolean result = gameListing.updateGame(API_KEY_0, GAME_ID_0, lobbyGame0);

      assertThat(result, is(false));
      verify(cache, never()).put(ID_0, lobbyGame0);
      verify(gameListingEventQueue, never()).gameUpdated(any());
    }

    @Test
    void updateGameThatDoesExist() {
      final Map<GameId, LobbyGame> map = new HashMap<>();
      map.put(ID_0, lobbyGame1);
      when(cache.asMap()).thenReturn(new ConcurrentHashMap<>(map));

      final boolean result = gameListing.updateGame(API_KEY_0, GAME_ID_0, lobbyGame0);

      assertThat(result, is(true));
      verify(gameListingEventQueue)
          .gameUpdated(LobbyGameListing.builder().gameId(GAME_ID_0).lobbyGame(lobbyGame0).build());
    }
  }

  @Nested
  final class BootGame {
    @Test
    void bootGame() {
      final Map<GameId, LobbyGame> games = new HashMap<>();
      games.put(ID_0, lobbyGame0);
      when(cache.asMap()).thenReturn(new ConcurrentHashMap<>(games));
      when(lobbyGame0.getHostName()).thenReturn(HOST_NAME);

      gameListing.bootGame(MODERATOR_ID, GAME_ID_0);

      verify(cache).invalidate(ID_0);
      verify(moderatorAuditHistoryDao)
          .addAuditRecord(
              ModeratorAuditHistoryDao.AuditArgs.builder()
                  .actionName(ModeratorAuditHistoryDao.AuditAction.BOOT_GAME)
                  .actionTarget(HOST_NAME)
                  .moderatorUserId(MODERATOR_ID)
                  .build());
      verify(gameListingEventQueue).gameRemoved(GAME_ID_0);
    }
  }
}
