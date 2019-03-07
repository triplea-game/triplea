package games.strategy.engine.lobby.client.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.Interruptibles;
import org.triplea.lobby.common.ILobbyGameController;
import org.triplea.swing.SwingAction;
import org.triplea.util.Tuple;

import com.google.common.util.concurrent.Runnables;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.Messengers;

final class LobbyGameTableModelTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class RemoveAndUpdateGameTest {
    private LobbyGameTableModel testObj;
    @Mock
    private IMessenger mockMessenger;
    @Mock
    private IChannelMessenger mockChannelMessenger;
    @Mock
    private IRemoteMessenger mockRemoteMessenger;
    @Mock
    private ILobbyGameController mockLobbyController;
    private Map<GUID, GameDescription> fakeGameMap;
    private Tuple<GUID, GameDescription> fakeGame;
    @Mock
    private GameDescription mockGameDescription;
    @Mock
    private INode serverNode;

    @BeforeEach
    void setUp() {
      fakeGameMap = new HashMap<>();
      fakeGame = Tuple.of(new GUID(), mockGameDescription);
      fakeGameMap.put(fakeGame.getFirst(), fakeGame.getSecond());

      Mockito.when(mockRemoteMessenger.getRemote(ILobbyGameController.REMOTE_NAME))
          .thenReturn(mockLobbyController);
      Mockito.when(mockLobbyController.listGames()).thenReturn(fakeGameMap);
      testObj = new LobbyGameTableModel(
          true, new Messengers(mockMessenger, mockRemoteMessenger, mockChannelMessenger));
      Mockito.verify(mockLobbyController, Mockito.times(1)).listGames();

      MessageContext.setSenderNodeForThread(serverNode);
      Mockito.when(mockMessenger.getServerNode()).thenReturn(serverNode);
      waitForSwingThreads();
      assertThat("games are loaded on init", testObj.getRowCount(), is(1));
    }

    private void waitForSwingThreads() {
      // add a no-op action to the end of the swing event queue, and then wait for it
      Interruptibles.await(() -> SwingAction.invokeAndWait(Runnables.doNothing()));
    }

    @Test
    void updateGame() {
      final int commentColumnIndex = testObj.getColumnIndex(LobbyGameTableModel.Column.Comments);
      assertThat(testObj.getValueAt(0, commentColumnIndex), nullValue());

      final String newComment = "comment";
      final GameDescription newDescription = new GameDescription();
      newDescription.setComment(newComment);

      testObj.getLobbyGameBroadcaster().gameUpdated(fakeGame.getFirst(), newDescription);
      waitForSwingThreads();
      assertThat(testObj.getRowCount(), is(1));
      assertThat(testObj.getValueAt(0, commentColumnIndex), is(newComment));
    }

    @Test
    void updateGameAddsIfDoesNotExist() {
      testObj.getLobbyGameBroadcaster().gameUpdated(new GUID(), new GameDescription());
      waitForSwingThreads();
      assertThat(testObj.getRowCount(), is(2));
    }

    @Test
    void updateGameWithNullGuidIsIgnored() {
      testObj.getLobbyGameBroadcaster().gameUpdated(null, new GameDescription());
      waitForSwingThreads();
      assertThat("expect row count to remain 1, null guid is bogus data",
          testObj.getRowCount(), is(1));
    }

    @Test
    void removeGame() {
      testObj.getLobbyGameBroadcaster().gameRemoved(fakeGame.getFirst());
      waitForSwingThreads();
      assertThat(testObj.getRowCount(), is(0));
    }

    @Test
    void removeGameThatDoesNotExistIsIgnored() {
      testObj.getLobbyGameBroadcaster().gameRemoved(new GUID());
      waitForSwingThreads();
      assertThat(testObj.getRowCount(), is(1));
    }
  }

  @Nested
  final class FormatBotStartTimeTest {
    @Test
    void shouldNotThrowException() {
      assertDoesNotThrow(() -> LobbyGameTableModel.formatBotStartTime(Instant.now()));
    }
  }
}
