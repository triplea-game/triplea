package games.strategy.engine.lobby.client.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.Runnables;
import games.strategy.net.Node;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.java.Interruptibles;
import org.triplea.lobby.common.GameDescription;
import org.triplea.swing.SwingAction;

@SuppressWarnings("InnerClassMayBeStatic")
@Execution(ExecutionMode.SAME_THREAD)
final class LobbyGameTableModelTest {

  @NonNls private static final String id0 = "id0";
  @NonNls private static final String id1 = "id1";
  private static final GameDescription gameDescription0;
  private static final GameDescription gameDescription1;

  static {
    try {
      gameDescription0 =
          GameDescription.builder()
              .hostedBy(new Node("node", InetAddress.getLocalHost(), 10))
              .startDateTime(Instant.now())
              .status(GameDescription.GameStatus.LAUNCHING)
              .build();

      gameDescription1 =
          GameDescription.builder()
              .hostedBy(new Node("another node", InetAddress.getLocalHost(), 10))
              .startDateTime(Instant.now())
              .status(GameDescription.GameStatus.IN_PROGRESS)
              .build();
    } catch (final UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class RemoveAndUpdateGameTest {
    private LobbyGameTableModel testObj;
    private LobbyGameListingModel listingModel;
    @Mock private PlayerToLobbyConnection playerToLobbyConnection;

    @BeforeEach
    void setUp() {
      listingModel = new LobbyGameListingModel(playerToLobbyConnection);
      testObj = new LobbyGameTableModel(true, listingModel);
      listingModel
          .getLobbyGameBroadcaster()
          .gameUpdated(
              LobbyGameListing.builder()
                  .gameId(id0)
                  .lobbyGame(gameDescription0.toLobbyGame())
                  .build());
      waitForSwingThreads();
    }

    private void waitForSwingThreads() {
      // add a no-op action to the end of the swing event queue, and then wait for it
      Interruptibles.await(() -> SwingAction.invokeAndWait(Runnables.doNothing()));
    }

    @Test
    void singleGameInModelAfterSetup() {
      assertThat(testObj.getRowCount()).as("games are loaded on init").isEqualTo(1);
    }

    @Test
    void updateGame() {
      final int commentColumnIndex = testObj.getColumnIndex(LobbyGameTableModel.Column.Comments);
      assertThat(testObj.getValueAt(0, commentColumnIndex)).isNull();

      listingModel
          .getLobbyGameBroadcaster()
          .gameUpdated(
              LobbyGameListing.builder()
                  .gameId(id0)
                  .lobbyGame(gameDescription1.toLobbyGame())
                  .build());

      waitForSwingThreads();
      assertThat(testObj.getRowCount()).isEqualTo(1);
      assertThat(testObj.getValueAt(0, commentColumnIndex))
          .isEqualTo(gameDescription1.getComment());
    }

    @Test
    void updateGameAddsIfDoesNotExist() {
      listingModel
          .getLobbyGameBroadcaster()
          .gameUpdated(
              LobbyGameListing.builder()
                  .gameId(id1)
                  .lobbyGame(gameDescription1.toLobbyGame())
                  .build());
      waitForSwingThreads();
      assertThat(testObj.getRowCount()).isEqualTo(2);
    }

    @Test
    void removeGame() {
      listingModel.getLobbyGameBroadcaster().gameRemoved(id0);
      waitForSwingThreads();
      assertThat(testObj.getRowCount()).isEqualTo(0);
    }

    @Test
    void removeGameThatDoesNotExistIsIgnored() {
      listingModel.getLobbyGameBroadcaster().gameRemoved(id1);
      waitForSwingThreads();
      assertThat(testObj.getRowCount()).isEqualTo(1);
    }

    @Test
    void statusShowsRealStatusForStartedZeroPlayerGame() throws UnknownHostException {
      updateGame(zeroPlayerGameAtRound(1));

      final int statusColumnIndex = testObj.getColumnIndex(LobbyGameTableModel.Column.Status);
      assertThat(testObj.getValueAt(0, statusColumnIndex))
          .isEqualTo(GameDescription.GameStatus.IN_PROGRESS.toString());
    }

    @Test
    void statusShowsAvailableForRoundZeroZeroPlayerGame() throws UnknownHostException {
      updateGame(zeroPlayerGameAtRound(0));

      final int statusColumnIndex = testObj.getColumnIndex(LobbyGameTableModel.Column.Status);
      assertThat(testObj.getValueAt(0, statusColumnIndex)).isEqualTo("Available");
    }

    private GameDescription zeroPlayerGameAtRound(final int round) throws UnknownHostException {
      return GameDescription.builder()
          .hostedBy(new Node("bot node", InetAddress.getLocalHost(), 10))
          .startDateTime(Instant.now())
          .status(GameDescription.GameStatus.IN_PROGRESS)
          .playerCount(0)
          .round(round)
          .build();
    }

    private void updateGame(final GameDescription game) {
      listingModel
          .getLobbyGameBroadcaster()
          .gameUpdated(
              LobbyGameListing.builder().gameId(id0).lobbyGame(game.toLobbyGame()).build());
      waitForSwingThreads();
    }
  }

  @Nested
  final class FormatBotStartTimeTest {
    @Test
    void shouldNotThrowException() {
      assertDoesNotThrow(
          () -> LobbyGameTableModel.formatBotStartTime(Instant.now().toEpochMilli()));
    }
  }

  @Nested
  final class StartTimeTest {
    @Test
    void sortsByEpochMillisNotFormattedText() {
      final var earlier = new LobbyGameTableModel.StartTime(1_000L);
      final var later = new LobbyGameTableModel.StartTime(2_000L);

      assertThat(earlier).isLessThan(later);
      assertThat(later).isGreaterThan(earlier);
      assertThat(earlier).isEqualByComparingTo(new LobbyGameTableModel.StartTime(1_000L));
    }

    @Test
    void startedColumnUsesStartTimeSoTheSorterComparesChronologically() {
      final var testObj =
          new LobbyGameTableModel(
              true, new LobbyGameListingModel(mock(PlayerToLobbyConnection.class)));

      final int startedColumnIndex = testObj.getColumnIndex(LobbyGameTableModel.Column.Started);

      assertThat(testObj.getColumnClass(startedColumnIndex))
          .isEqualTo(LobbyGameTableModel.StartTime.class);
    }
  }
}
