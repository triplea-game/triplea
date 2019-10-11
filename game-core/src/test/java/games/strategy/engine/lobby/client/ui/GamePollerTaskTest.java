package games.strategy.engine.lobby.client.ui;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.lobby.common.LobbyGameUpdateListener;
import org.triplea.test.TestData;

@ExtendWith(MockitoExtension.class)
class GamePollerTaskTest {

  private static final String ID_0 = "id0";
  private static final String ID_1 = "id1";
  private static final String ID_2 = "id2";
  private static final String ID_3 = "id3";

  private static final LobbyGame GAME_0 = TestData.LOBBY_GAME;

  private static final LobbyGame GAME_1 = GAME_0.withComments("comments1");
  private static final LobbyGame GAME_2 = GAME_0.withComments("");
  private static final LobbyGame GAME_3 = GAME_0.withComments("comments3");

  private static final LobbyGameListing LISTING_0 =
      LobbyGameListing.builder().gameId(ID_0).lobbyGame(GAME_0).build();

  private static final LobbyGameListing LISTING_1 =
      LobbyGameListing.builder().gameId(ID_1).lobbyGame(GAME_1).build();

  private static final LobbyGameListing LISTING_3 =
      LobbyGameListing.builder().gameId(ID_3).lobbyGame(GAME_3).build();

  @Mock private LobbyGameUpdateListener lobbyGameBroadcaster;
  @Mock private Supplier<Map<String, LobbyGame>> localGameListingFetcher;
  @Mock private Supplier<List<LobbyGameListing>> lobbyGameListingFetcher;
  @Mock private Consumer<String> errorReporter;

  private GamePollerTask gamePollerTask;

  @Mock private FeignException feignException;

  @BeforeEach
  void setup() {
    gamePollerTask =
        new GamePollerTask(
            lobbyGameBroadcaster, localGameListingFetcher, lobbyGameListingFetcher, errorReporter);
  }

  /** No games in model, listing returns 1 game, should be found as new. */
  @Test
  void newGame() {
    when(localGameListingFetcher.get()).thenReturn(Collections.emptyMap());
    when(lobbyGameListingFetcher.get())
        .thenReturn(
            asList(
                LobbyGameListing.builder().gameId(ID_0).lobbyGame(GAME_0).build(),
                LobbyGameListing.builder().gameId(ID_1).lobbyGame(GAME_1).build()));

    gamePollerTask.run();

    verify(lobbyGameBroadcaster).gameUpdated(LISTING_0);
    verify(lobbyGameBroadcaster).gameUpdated(LISTING_1);
  }

  /** One game in model, listing returns zero games, game in model should be removed. */
  @Test
  void removedGame() {
    when(localGameListingFetcher.get()).thenReturn(Map.of(ID_0, GAME_0, ID_1, GAME_1));
    when(lobbyGameListingFetcher.get()).thenReturn(Collections.emptyList());

    gamePollerTask.run();

    verify(lobbyGameBroadcaster).gameRemoved(ID_0);
    verify(lobbyGameBroadcaster).gameRemoved(ID_1);
  }

  /** One game in model, listing returns different game with same ID, should be updated. */
  @Test
  void gameUpdated() {
    when(localGameListingFetcher.get()).thenReturn(Map.of(ID_0, GAME_0));
    when(lobbyGameListingFetcher.get())
        .thenReturn(
            singletonList(LobbyGameListing.builder().gameId(ID_0).lobbyGame(GAME_1).build()));

    gamePollerTask.run();

    verify(lobbyGameBroadcaster)
        .gameUpdated(LobbyGameListing.builder().gameId(ID_0).lobbyGame(GAME_1).build());
  }

  /** One game in model, listing returns equal game object with same ID, should not be updated. */
  @Test
  void noGameUpdates() {
    when(localGameListingFetcher.get()).thenReturn(Map.of(ID_0, GAME_0));
    final LobbyGame gameEqualToGame0 = GAME_0.withComments(GAME_0.getComments());
    when(lobbyGameListingFetcher.get())
        .thenReturn(
            singletonList(
                LobbyGameListing.builder().gameId(ID_0).lobbyGame(gameEqualToGame0).build()));

    gamePollerTask.run();

    verify(lobbyGameBroadcaster, never()).gameUpdated(any());
  }

  @Test
  void mixtureOfUpdatesAndNewGamesAndRemoved() {
    when(localGameListingFetcher.get())
        .thenReturn(Map.of(ID_0, GAME_0, ID_1, GAME_1, ID_2, GAME_2));
    when(lobbyGameListingFetcher.get())
        .thenReturn(
            asList(
                // id0 is not updated
                LobbyGameListing.builder().gameId(ID_0).lobbyGame(GAME_0).build(),
                // id1 is updated
                LobbyGameListing.builder().gameId(ID_1).lobbyGame(GAME_2).build(),
                // id2 is removed
                // id3 is new
                LobbyGameListing.builder().gameId(ID_3).lobbyGame(GAME_3).build()));

    gamePollerTask.run();

    verify(lobbyGameBroadcaster)
        .gameUpdated(LobbyGameListing.builder().gameId(ID_1).lobbyGame(GAME_2).build());
    verify(lobbyGameBroadcaster).gameRemoved(ID_2);
    verify(lobbyGameBroadcaster).gameUpdated(LISTING_3);
  }

  @Nested
  class ErrorReporting {
    @Test
    void onlyReportErrorsIfThereIsASuccess() {
      when(lobbyGameListingFetcher.get()).thenThrow(feignException);

      gamePollerTask.run();

      verify(errorReporter, never()).accept(any());
    }

    @Test
    void reportsErrorAfterFirstSuccess() {
      when(localGameListingFetcher.get()).thenReturn(Collections.emptyMap());
      when(lobbyGameListingFetcher.get())
          .thenReturn(Collections.emptyList())
          .thenThrow(feignException);

      gamePollerTask.run();
      gamePollerTask.run();

      verify(errorReporter).accept(any());
    }

    @Test
    void reportsErrorOnlyOnce() {
      when(localGameListingFetcher.get()).thenReturn(Collections.emptyMap());
      when(lobbyGameListingFetcher.get())
          .thenReturn(Collections.emptyList())
          .thenThrow(feignException)
          .thenThrow(feignException);

      gamePollerTask.run();
      gamePollerTask.run();
      gamePollerTask.run();

      verify(errorReporter).accept(any());
    }

    @Test
    void reportsErrorAfterRecoveryAndSubsequentError() {
      when(localGameListingFetcher.get()).thenReturn(Collections.emptyMap());
      when(lobbyGameListingFetcher.get())
          .thenReturn(Collections.emptyList())
          .thenThrow(feignException)
          .thenReturn(Collections.emptyList())
          .thenThrow(feignException);

      gamePollerTask.run();
      gamePollerTask.run();
      gamePollerTask.run();
      gamePollerTask.run();

      verify(errorReporter, times(2)).accept(any());
    }
  }
}
