package games.strategy.engine.lobby.client.ui;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
class GamePollerTaskTest {

  private static final String id0 = "id0";
  private static final String id1 = "id1";
  private static final String id2 = "id2";
  private static final String id3 = "id3";

  private LobbyGame game0 =
      LobbyGame.builder()
          .hostAddress("127.0.0.1")
          .hostPort(12)
          .hostName("name")
          .mapName("map")
          .playerCount(3)
          .gameRound(1)
          .epochMilliTimeStarted(Instant.now().toEpochMilli())
          .mapVersion("1")
          .passworded(false)
          .status("WAITING_FOR_PLAYERS")
          .comments("comments")
          .build();

  private LobbyGame game1 = game0.withComments("comments1");
  private LobbyGame game2 = game0.withComments("");
  private LobbyGame game3 = game0.withComments("comments3");

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
                LobbyGameListing.builder().gameId(id0).lobbyGame(game0).build(),
                LobbyGameListing.builder().gameId(id1).lobbyGame(game1).build()));

    gamePollerTask.run();

    verify(lobbyGameBroadcaster).gameUpdated(id0, game0);
    verify(lobbyGameBroadcaster).gameUpdated(id1, game1);
  }

  /** One game in model, listing returns zero games, game in model should be removed. */
  @Test
  void removedGame() {
    when(localGameListingFetcher.get()).thenReturn(Map.of(id0, game0, id1, game1));
    when(lobbyGameListingFetcher.get()).thenReturn(Collections.emptyList());

    gamePollerTask.run();

    verify(lobbyGameBroadcaster).gameRemoved(id0);
    verify(lobbyGameBroadcaster).gameRemoved(id1);
  }

  /** One game in model, listing returns different game with same ID, should be updated. */
  @Test
  void gameUpdated() {
    when(localGameListingFetcher.get()).thenReturn(Map.of(id0, game0));
    when(lobbyGameListingFetcher.get())
        .thenReturn(singletonList(LobbyGameListing.builder().gameId(id0).lobbyGame(game1).build()));

    gamePollerTask.run();

    verify(lobbyGameBroadcaster).gameUpdated(id0, game1);
  }

  /** One game in model, listing returns equal game object with same ID, should not be updated. */
  @Test
  void noGameUpdates() {
    when(localGameListingFetcher.get()).thenReturn(Map.of(id0, game0));
    final LobbyGame gameEqualToGame0 = game0.withComments(game0.getComments());
    when(lobbyGameListingFetcher.get())
        .thenReturn(
            singletonList(
                LobbyGameListing.builder().gameId(id0).lobbyGame(gameEqualToGame0).build()));

    gamePollerTask.run();

    verify(lobbyGameBroadcaster, never()).gameUpdated(any(), any());
  }

  @Test
  void mixtureOfUpdatesAndNewGamesAndRemoved() {
    when(localGameListingFetcher.get()).thenReturn(Map.of(id0, game0, id1, game1, id2, game2));
    when(lobbyGameListingFetcher.get())
        .thenReturn(
            asList(
                // id0 is not updated
                LobbyGameListing.builder().gameId(id0).lobbyGame(game0).build(),
                // id1 is updated
                LobbyGameListing.builder().gameId(id1).lobbyGame(game2).build(),
                // id2 is removed
                // id3 is new
                LobbyGameListing.builder().gameId(id3).lobbyGame(game3).build()));

    gamePollerTask.run();

    verify(lobbyGameBroadcaster, never()).gameUpdated(eq(id0), any());
    verify(lobbyGameBroadcaster).gameUpdated(id1, game2);
    verify(lobbyGameBroadcaster).gameRemoved(id2);
    verify(lobbyGameBroadcaster).gameUpdated(id3, game3);
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
