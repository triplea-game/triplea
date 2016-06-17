package games.strategy.engine.lobby.client.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.ILobbyGameController;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.test.TestUtil;
import games.strategy.util.Tuple;

@RunWith(MockitoJUnitRunner.class)
public class LobbyGameTableModelTest {

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

  @Before
  public void setUp() {
    fakeGameMap = new HashMap<>();
    fakeGame = Tuple.of(new GUID(), mockGameDescription);
    fakeGameMap.put(fakeGame.getFirst(), fakeGame.getSecond());

    Mockito.when(mockRemoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE))
        .thenReturn(mockLobbyController);
    Mockito.when(mockLobbyController.listGames()).thenReturn(fakeGameMap);
    testObj = new LobbyGameTableModel(mockMessenger, mockChannelMessenger, mockRemoteMessenger);
    Mockito.verify(mockLobbyController, Mockito.times(1)).listGames();


    MessageContext.setSenderNodeForThread(serverNode);
    Mockito.when(mockMessenger.getServerNode()).thenReturn(serverNode);
    TestUtil.waitForSwingThreads();
  }

  @Test
  public void gamesAreLoadedOnInit() {
    assertThat(testObj.getRowCount(), is(1));
  }

  @Test
  public void updateGame() {
    int commentColumnIndex = testObj.getColumnIndex(LobbyGameTableModel.Column.Comments);
    assertThat((String) testObj.getValueAt(0, commentColumnIndex), nullValue());

    String newComment = "comment";
    GameDescription newDescription = new GameDescription();
    newDescription.setComment(newComment);

    testObj.getLobbyGameBroadcaster().gameUpdated(fakeGame.getFirst(), newDescription);
    TestUtil.waitForSwingThreads();
    assertEquals(1, testObj.getRowCount());
    assertEquals(newComment, testObj.getValueAt(0, commentColumnIndex));
  }

  @Test
  public void updateGameAddsIfDoesNotExist() {
    testObj.getLobbyGameBroadcaster().gameUpdated(new GUID(), new GameDescription());
    TestUtil.waitForSwingThreads();
    assertEquals(2, testObj.getRowCount());
  }

  @Test
  public void updateGameWithNullGuidIsIgnored() {
    testObj.getLobbyGameBroadcaster().gameUpdated(null, new GameDescription());
    TestUtil.waitForSwingThreads();
    assertThat("expect row count to remain 1, null guid is bogus data",
        testObj.getRowCount(), is(1));
  }

  @Test
  public void removeGame() {
    testObj.getLobbyGameBroadcaster().gameRemoved(fakeGame.getFirst());
    TestUtil.waitForSwingThreads();
    assertEquals(0, testObj.getRowCount());
  }

  @Test
  public void removeGameThatDoesNotExistIsIgnored() {
    testObj.getLobbyGameBroadcaster().gameRemoved(new GUID());
    TestUtil.waitForSwingThreads();
    assertEquals(1, testObj.getRowCount());
  }
}
