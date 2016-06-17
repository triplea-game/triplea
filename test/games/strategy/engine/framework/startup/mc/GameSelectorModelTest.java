package games.strategy.engine.framework.startup.mc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Observable;
import java.util.Observer;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.framework.ui.NewGameChooserEntry;
import games.strategy.util.Version;


@RunWith(MockitoJUnitRunner.class)
public class GameSelectorModelTest {

  private static void assertHasEmptyData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getGameData(), Matchers.nullValue());
    assertHasEmptyDisplayData(objectToCheck);
  }

  private static void assertHasEmptyDisplayData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getFileName(), Matchers.is("-"));
    assertThat(objectToCheck.getGameName(), Matchers.is("-"));
    assertThat(objectToCheck.getGameRound(), Matchers.is("-"));
  }

  private static void assertHasFakeTestData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getGameName(), Matchers.is(fakeGameName));
    assertThat(objectToCheck.getGameRound(), Matchers.is(fakeGameRound));
    assertThat(objectToCheck.getGameVersion(), Matchers.is(fakeGameVersion));
  }

  private static final String fakeGameVersion = "fakeGameVersion";
  private static final String fakeGameRound = "3";
  private static final String fakeGameName = "_fakeGameName_";
  private static final String fakeFileName = "/hack/and/slash";



  private GameSelectorModel testObj;

  @Mock
  private NewGameChooserEntry mockEntry;

  @Mock
  private GameData mockGameData;

  @Mock
  private Version mockVersion;

  @Mock
  private GameSequence mockSequence;

  @Mock
  private Observer mockObserver;

  @Mock
  private ClientModel mockClientModel;

  @Mock
  private Component mockUiComponent;


  @Before
  public void setup() {
    testObj = new GameSelectorModel();
    assertHasEmptyData(testObj);
    testObj.addObserver(mockObserver);
  }

  @After
  public void tearDown() {
    testObj.deleteObservers();
  }

  @Test
  public void testSetGameData() {
    assertHasEmptyData(testObj);
    this.testObjectSetMockGameData();
  }


  private final void testObjectSetMockGameData() {
    prepareMockGameDataExpectations();
    testObj.setGameData(mockGameData);
    assertThat(testObj.getGameData(), Matchers.sameInstance(mockGameData));
    assertHasFakeTestData(testObj);
    this.verifyTestObjectObserverUpdateSent();
  }

  private void verifyTestObjectObserverUpdateSent() {
    verify(mockObserver, times(1)).update((Observable) Mockito.any(), Mockito.any());
    reset(mockObserver);
  }

  private final void prepareMockGameDataExpectations() {
    when(mockGameData.getGameVersion()).thenReturn(mockVersion);
    when(mockVersion.toString()).thenReturn(fakeGameVersion);
    when(mockGameData.getSequence()).thenReturn(mockSequence);
    when(mockSequence.getRound()).thenReturn(Integer.valueOf(fakeGameRound));
    when(mockGameData.getGameName()).thenReturn(fakeGameName);
  }

  @Test
  public void testResetGameDataToNull() {
    assertHasEmptyData(testObj);
    this.testObjectSetMockGameData();

    testObj.resetGameDataToNull();
    assertHasEmptyData(testObj);
  }


  @Test
  public void testIsSaveGame() {
    testObj.load((GameData) null, "");
    assertThat(testObj.isSavedGame(), Matchers.is(true));

    testObj.load((GameData) null, ".xml");
    assertThat(testObj.isSavedGame(), Matchers.is(false));

    testObj.load((GameData) null, "file.tsvg");
    assertThat(testObj.isSavedGame(), Matchers.is(true));
  }


  @Test
  public void testCanSelect() {
    assertThat(testObj.canSelect(), Matchers.is(true));
    testObj.setCanSelect(false);
    assertThat(testObj.canSelect(), Matchers.is(false));
    testObj.setCanSelect(true);
    assertThat(testObj.canSelect(), Matchers.is(true));
  }


  @Test
  public void testClearDataButKeepGameInfo() {
    this.testObjectSetMockGameData();

    final String newGameName = " 123";
    final String newGameRound = "gameRound xyz";
    final String newGameVersion = "gameVersion abc";

    testObj.clearDataButKeepGameInfo(newGameName, newGameRound, newGameVersion);
    verifyTestObjectObserverUpdateSent();
    assertThat(testObj.getGameData(), Matchers.nullValue());
    assertThat(testObj.getGameName(), Matchers.is(newGameName));
    assertThat(testObj.getGameRound(), Matchers.is(newGameRound));
    assertThat(testObj.getGameVersion(), Matchers.is(newGameVersion));

  }

  @Test
  public void testLoadFromNewGameChooserEntry() {
    final String fileName = "testname";
    when(mockEntry.getLocation()).thenReturn(fileName);

    when(mockEntry.getGameData()).thenReturn(mockGameData);
    prepareMockGameDataExpectations();
    try {
      when(mockEntry.getURI()).thenReturn(new URI("abc"));
    } catch (final URISyntaxException e) {
      throw new RuntimeException(e);
    }
    testObj.load(mockEntry);
    assertThat(testObj.getFileName(), Matchers.is(fileName));

    assertThat(testObj.getGameData(), Matchers.sameInstance(mockGameData));
    assertHasFakeTestData(testObj);
  }


  @Test
  public void testLoadFromGameDataFileNamePair() {
    assertHasEmptyData(testObj);

    prepareMockGameDataExpectations();
    testObj.load(mockGameData, fakeFileName);
    assertThat(testObj.getGameData(), Matchers.sameInstance(mockGameData));
    assertThat(testObj.getFileName(), Matchers.is(fakeFileName));
  }


  @Test
  public void testGetGameData() {
    assertThat(testObj.getGameData(), Matchers.nullValue());
    prepareMockGameDataExpectations();
    testObj.setGameData(mockGameData);
    assertThat(testObj.getGameData(), Matchers.sameInstance(mockGameData));
  }

  @Test
  public void testSetAndGetIsHostHeadlessBot() {
    assertFalse(testObj.isHostHeadlessBot());
    testObj.setIsHostHeadlessBot(true);
    assertTrue(testObj.isHostHeadlessBot());
    testObj.setIsHostHeadlessBot(false);
    assertFalse(testObj.isHostHeadlessBot());
  }


  @Test
  public void testSetAndGetClientModelForHostBots() {
    assertThat(testObj.getClientModelForHostBots(), Matchers.nullValue());
    testObj.setClientModelForHostBots(mockClientModel);
    assertThat(testObj.getClientModelForHostBots(), Matchers.sameInstance(mockClientModel));
    testObj.setClientModelForHostBots(null);
    assertThat(testObj.getClientModelForHostBots(), Matchers.nullValue());
  }

  @Test
  public void testGetFileName() {
    assertThat(testObj.getFileName(), Matchers.is("-"));
    prepareMockGameDataExpectations();
    testObj.load(mockGameData, fakeFileName);
    assertThat(testObj.getFileName(), Matchers.is(fakeFileName));
    testObj.resetGameDataToNull();
    assertThat(testObj.getFileName(), Matchers.is("-"));
  }

  @Test
  public void testGetGameName() {
    this.testObjectSetMockGameData();
    assertThat(testObj.getGameName(), Matchers.is(fakeGameName));
  }

  @Test
  public void testGetGameRound() {
    this.testObjectSetMockGameData();
    assertThat(testObj.getGameRound(), Matchers.is(fakeGameRound));
  }

  @Test
  public void testGetGameVersion() {
    this.testObjectSetMockGameData();
    assertThat(testObj.getGameVersion(), Matchers.is(fakeGameVersion));
  }

  @Ignore
  @Test
  public void testLoadFromInputStream() {
    // testObj.load(InputStream, string fileName);
    // TODO
  }


  @Ignore
  @Test
  public void testLoadFromFile() {
    // testObj.load(File, Component);
    // TODO
  }

  @Ignore
  @Test
  public void testLoadDefaultGame() {
    // testObj.loadDefaultGame(Component);
    // TODO
  }

}
