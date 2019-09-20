package org.triplea.server.lobby.game.listing;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;

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
  private static final String GAME_ID = "id0";

  private static final String API_KEY_0 = "apiKey0";
  private static final String API_KEY_1 = "apiKey1";

  private static final String HOST_NAME = "host-player";
  private static final int MODERATOR_ID = 33;

  @Mock private Consumer<LobbyGameListing> gameUpdateListener;
  @Mock private Consumer<String> gameRemoveListener;
  @Mock private GameReaper gameReaper;
  @Mock private ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  private GameListing gameListing;

  @Mock private LobbyGame lobbyGame0;
  @Mock private LobbyGame lobbyGame1;
  @Mock private LobbyGame lobbyGame2;

  @BeforeEach
  void setup() {
    gameListing =
        GameListing.builder()
            .gameReaper(gameReaper)
            .gameUpdateListener(gameUpdateListener)
            .gameRemoveListener(gameRemoveListener)
            .auditHistoryDao(moderatorAuditHistoryDao)
            .build();
  }

  @Nested
  final class GetGames {
    /** Basic case, no games added, expect none to be returned. */
    @Test
    void getGamesEmptyCase() {
      when(gameReaper.findDeadGames(any())).thenReturn(emptyList());

      final var games = gameListing.getGames();

      assertThat("Without adding any games, getGames() should return empty", games, empty());
      verify(gameReaper).findDeadGames(any());
    }

    /** Add one game, reap none, expect one game to be returned. */
    @Test
    void getGamesSingletonGame() {
      when(gameReaper.findDeadGames(any())).thenReturn(emptyList());
      gameListing.postGame(API_KEY_0, lobbyGame0);

      final var games = gameListing.getGames();

      assertThat("We added one game, expect there to be one game returned", games, hasSize(1));
      assertThat(
          "We expect the one game to be the same one we added",
          games.get(0).getLobbyGame(),
          sameInstance(lobbyGame0));
    }

    /**
     * Add 3 games, set the reaper to remove 2 of those games, we expected one game to come back.
     */
    @Test
    void getGamesWithReaper() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);
      final String id1 = gameListing.postGame(API_KEY_0, lobbyGame1);
      final String id2 = gameListing.postGame(API_KEY_0, lobbyGame2);
      when(gameReaper.findDeadGames(any()))
          .thenReturn(
              Arrays.asList(
                  GameListing.GameId.builder().apiKey(API_KEY_0).id(id0).build(),
                  GameListing.GameId.builder().apiKey(API_KEY_0).id(id1).build()));

      final var games = gameListing.getGames();

      assertThat(
          "Reaper returned two out of three games to be reaped, we expect "
              + "only one game to remain.",
          games,
          hasSize(1));
      assertThat(games.get(0).getGameId(), is(id2));
      assertThat(games.get(0).getLobbyGame(), sameInstance(lobbyGame2));

      verify(gameRemoveListener).accept(id0);
      verify(gameRemoveListener).accept(id1);
    }
  }

  @Nested
  final class KeepAlive {
    @Test
    void noGamesPresent() {
      final boolean result = gameListing.keepAlive(API_KEY_0, GAME_ID);

      assertThat("no games added, keep alive should not match anything", result, is(false));
      // game is not there, do not call reaper as the game was not found (already dead)
      verify(gameReaper, never()).registerKeepAlive(any());
    }

    @Test
    void mismatchingGameId() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      final boolean result = gameListing.keepAlive(API_KEY_0, GAME_ID);

      assertThat("game id does not match, expecting false", result, is(false));
      verify(gameReaper).registerKeepAlive(id0);
    }

    @Test
    void mismatchingApiKey() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      assertThrows(GameListing.IncorrectApiKey.class, () -> gameListing.keepAlive(API_KEY_1, id0));

      verify(gameReaper).registerKeepAlive(id0);
    }

    @Test
    void correctApiKeyAndGameId() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      final boolean result = gameListing.keepAlive(API_KEY_0, id0);

      assertThat(result, is(true));

      // keep alive called on insert (post) and then again on the explicit keep alive call
      verify(gameReaper, times(2)).registerKeepAlive(id0);
    }
  }

  @Nested
  final class RemoveGame {
    @Test
    void removeGameWithNoGames() {
      gameListing.removeGame(API_KEY_0, GAME_ID);

      verify(gameUpdateListener, never()).accept(any());
      verify(gameRemoveListener, never()).accept(any());
    }

    @Test
    void withMatchingApiKeyRemoveGame() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      gameListing.removeGame(API_KEY_0, id0);

      assertThat(
          "The one game added should be removed leaving no games", gameListing.getGames(), empty());
      verify(gameRemoveListener).accept(id0);
    }

    @Test
    void mismatchingApiKeyDoesNothingOnRemove() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      assertThrows(GameListing.IncorrectApiKey.class, () -> gameListing.removeGame(API_KEY_1, id0));

      assertThat("The one game added should remain", gameListing.getGames(), hasSize(1));
      verify(gameRemoveListener, never()).accept(any());
    }
  }

  @Nested
  final class PostGame {
    @Test
    void postGameAddsGame() {
      assertThat(gameListing.getGames(), empty());

      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      assertThat(id0, not(emptyString()));
      assertThat(gameListing.getGames(), hasSize(1));
      assertThat(gameListing.getGames().get(0).getLobbyGame(), sameInstance(lobbyGame0));
      assertThat(gameListing.getGames().get(0).getGameId(), is(id0));
      verify(gameUpdateListener)
          .accept(LobbyGameListing.builder().gameId(id0).lobbyGame(lobbyGame0).build());
      verify(gameReaper).registerKeepAlive(id0);
    }

    @Test
    void dupeGamePostIsNoOp() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);
      final String id1 = gameListing.postGame(API_KEY_0, lobbyGame0);

      assertThat(
          "Second posting should be idempotent returning same gameId already posted", id0, is(id1));
      assertThat(gameListing.getGames(), hasSize(1));
      // expecting just the one call to updateListener and not two
      verify(gameUpdateListener)
          .accept(LobbyGameListing.builder().gameId(id0).lobbyGame(lobbyGame0).build());
    }

    @Test
    void newApiKeyCreatesNewGame() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);
      final String id1 = gameListing.postGame(API_KEY_1, lobbyGame0);

      assertThat(
          "Even though posting the same game data, differing API keys will cause new game posts",
          id0,
          not(is(id1)));
      assertThat(gameListing.getGames(), hasSize(2));
      verify(gameUpdateListener)
          .accept(LobbyGameListing.builder().gameId(id0).lobbyGame(lobbyGame0).build());
      verify(gameUpdateListener)
          .accept(LobbyGameListing.builder().gameId(id1).lobbyGame(lobbyGame0).build());
      verify(gameReaper).registerKeepAlive(id0);
      verify(gameReaper).registerKeepAlive(id1);
    }
  }

  @Nested
  final class UpdateGame {
    @Test
    void updateGameThatDoesNotExist() {
      assertThrows(
          GameListing.GameNotFound.class,
          () -> gameListing.updateGame(API_KEY_0, GAME_ID, lobbyGame0));
      verify(gameUpdateListener, never()).accept(any());
      verify(gameRemoveListener, never()).accept(any());
    }

    @Test
    void updateGameWithIncorrectGameId() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      assertThrows(
          GameListing.GameNotFound.class,
          () -> gameListing.updateGame(API_KEY_0, GAME_ID, lobbyGame1));
      verify(gameUpdateListener)
          .accept(LobbyGameListing.builder().gameId(id0).lobbyGame(lobbyGame0).build());
      verify(gameRemoveListener, never()).accept(any());
    }

    @Test
    void updateGameWithIncorrectApiKey() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      assertThrows(
          GameListing.IncorrectApiKey.class,
          () -> gameListing.updateGame(API_KEY_1, id0, lobbyGame1));
      // first update listener we expect to be called by the first game posting.
      verify(gameUpdateListener)
          .accept(LobbyGameListing.builder().gameId(id0).lobbyGame(lobbyGame0).build());
      verify(gameRemoveListener, never()).accept(any());
    }

    @Test
    void updateGameThatExists() {
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      gameListing.updateGame(API_KEY_0, id0, lobbyGame1);

      verify(gameUpdateListener)
          .accept(LobbyGameListing.builder().gameId(id0).lobbyGame(lobbyGame1).build());
    }
  }

  @Nested
  final class BootGame {
    @Test
    void bootGameNoGames() {
      assertThrows(
          GameListing.GameNotFound.class, () -> gameListing.bootGame(MODERATOR_ID, GAME_ID));

      verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
      verify(gameUpdateListener, never()).accept(any());
      verify(gameRemoveListener, never()).accept(any());
    }

    @Test
    void bootGameNotFound() {
      gameListing.postGame(API_KEY_0, lobbyGame0);

      assertThrows(
          GameListing.GameNotFound.class, () -> gameListing.bootGame(MODERATOR_ID, GAME_ID));

      verify(moderatorAuditHistoryDao, never()).addAuditRecord(any());
      verify(gameRemoveListener, never()).accept(any());
    }

    @Test
    void bootGameFound() {
      when(lobbyGame0.getHostName()).thenReturn(HOST_NAME);
      final String id0 = gameListing.postGame(API_KEY_0, lobbyGame0);

      gameListing.bootGame(MODERATOR_ID, id0);

      assertThat(
          "We had one game added, one booted, no games should remain",
          gameListing.getGames(),
          empty());
      verify(moderatorAuditHistoryDao)
          .addAuditRecord(
              ModeratorAuditHistoryDao.AuditArgs.builder()
                  .actionName(ModeratorAuditHistoryDao.AuditAction.BOOT_GAME)
                  .actionTarget(HOST_NAME)
                  .moderatorUserId(MODERATOR_ID)
                  .build());
      verify(gameRemoveListener).accept(id0);
    }
  }
}
