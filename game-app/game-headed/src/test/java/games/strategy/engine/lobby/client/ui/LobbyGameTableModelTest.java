package games.strategy.engine.lobby.client.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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

  @NonNls
  private static final String id0 = "id0";
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
    @Mock private PlayerToLobbyConnection playerToLobbyConnection;

    @BeforeEach
    void setUp() {
      testObj = new LobbyGameTableModel(true, playerToLobbyConnection);
      testObj
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
      assertThat("games are loaded on init", testObj.getRowCount(), is(1));
    }

    @Test
    void updateGame() {
      final int commentColumnIndex = testObj.getColumnIndex(LobbyGameTableModel.Column.Comments);
      assertThat(testObj.getValueAt(0, commentColumnIndex), nullValue());

      testObj
          .getLobbyGameBroadcaster()
          .gameUpdated(
              LobbyGameListing.builder()
                  .gameId(id0)
                  .lobbyGame(gameDescription1.toLobbyGame())
                  .build());

      waitForSwingThreads();
      assertThat(testObj.getRowCount(), is(1));
      assertThat(testObj.getValueAt(0, commentColumnIndex), is(gameDescription1.getComment()));
    }

    @Test
    void updateGameAddsIfDoesNotExist() {
      testObj
          .getLobbyGameBroadcaster()
          .gameUpdated(
              LobbyGameListing.builder()
                  .gameId(id1)
                  .lobbyGame(gameDescription1.toLobbyGame())
                  .build());
      waitForSwingThreads();
      assertThat(testObj.getRowCount(), is(2));
    }

    @Test
    void removeGame() {
      testObj.getLobbyGameBroadcaster().gameRemoved(id0);
      waitForSwingThreads();
      assertThat(testObj.getRowCount(), is(0));
    }

    @Test
    void removeGameThatDoesNotExistIsIgnored() {
      testObj.getLobbyGameBroadcaster().gameRemoved(id1);
      waitForSwingThreads();
      assertThat(testObj.getRowCount(), is(1));
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
}
