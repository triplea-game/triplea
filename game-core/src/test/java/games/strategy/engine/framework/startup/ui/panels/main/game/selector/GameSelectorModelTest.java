package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.net.URI;
import java.util.Observer;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.util.Version;

@ExtendWith(MockitoExtension.class)
class GameSelectorModelTest extends AbstractClientSettingTestCase {
  private static final String fakeGameVersion = "12.34.56";
  private static final String fakeGameRound = "3";
  private static final String fakeGameName = "_fakeGameName_";

  private GameSelectorModel testObj;

  @Mock private GameData mockGameData;

  @Mock private GameSequence mockSequence;

  @Mock private Observer mockObserver;

  @Mock private ClientModel mockClientModel;

  private static void assertHasEmptyData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getGameData(), nullValue());
    assertHasEmptyDisplayData(objectToCheck);
  }

  private static void assertHasEmptyDisplayData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getFileName(), is("-"));
    assertThat(objectToCheck.getGameName(), is("-"));
    assertThat(objectToCheck.getGameRound(), is("-"));
  }

  private static void assertHasFakeTestData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getGameName(), is(fakeGameName));
    assertThat(objectToCheck.getGameRound(), is(fakeGameRound));
    assertThat(objectToCheck.getGameVersion(), is(fakeGameVersion));
  }

  @BeforeEach
  void setUp() {
    testObj = new GameSelectorModel();
    assertHasEmptyData(testObj);
    testObj.addObserver(mockObserver);
  }

  @AfterEach
  void tearDown() {
    testObj.deleteObservers();
  }

  @Test
  void testSetGameData() {
    assertHasEmptyData(testObj);
    this.testObjectSetMockGameData();
  }

  private void testObjectSetMockGameData() {
    prepareMockGameDataExpectations();
    testObj.setGameData(mockGameData);
    assertThat(testObj.getGameData(), sameInstance(mockGameData));
    assertHasFakeTestData(testObj);
    this.verifyTestObjectObserverUpdateSent();
  }

  private void verifyTestObjectObserverUpdateSent() {
    verify(mockObserver, times(1)).update(Mockito.any(), Mockito.any());
    reset(mockObserver);
  }

  private void prepareMockGameDataExpectations() {
    when(mockGameData.getGameVersion()).thenReturn(new Version(fakeGameVersion));
    when(mockGameData.getSequence()).thenReturn(mockSequence);
    when(mockSequence.getRound()).thenReturn(Integer.valueOf(fakeGameRound));
    when(mockGameData.getGameName()).thenReturn(fakeGameName);
  }

  @Test
  void testCanSelect() {
    assertThat(testObj.isCanSelect(), is(true));
    testObj.setCanSelect(false);
    assertThat(testObj.isCanSelect(), is(false));
    testObj.setCanSelect(true);
    assertThat(testObj.isCanSelect(), is(true));
  }

  @Test
  void testClearDataButKeepGameInfo() {
    this.testObjectSetMockGameData();

    final String newGameName = " 123";
    final String newGameRound = "gameRound xyz";
    final String newGameVersion = "gameVersion abc";

    testObj.clearDataButKeepGameInfo(newGameName, newGameRound, newGameVersion);
    verifyTestObjectObserverUpdateSent();
    assertThat(testObj.getGameData(), nullValue());
    assertThat(testObj.getGameName(), is(newGameName));
    assertThat(testObj.getGameRound(), is(newGameRound));
    assertThat(testObj.getGameVersion(), is(newGameVersion));
  }

  @Test
  void testLoadFromNewGameChooserEntry() throws Exception {
    prepareMockGameDataExpectations();
    testObj = new GameSelectorModel(uri -> Optional.of(mockGameData));

    testObj.load(new URI("abc"));

    assertThat(testObj.getFileName(), is("-"));
    assertThat(testObj.getGameData(), sameInstance(mockGameData));
    assertHasFakeTestData(testObj);
  }

  @Test
  void testGetGameData() {
    assertThat(testObj.getGameData(), nullValue());
    prepareMockGameDataExpectations();
    testObj.setGameData(mockGameData);
    assertThat(testObj.getGameData(), sameInstance(mockGameData));
  }

  @Test
  void testSetAndGetIsHostHeadlessBot() {
    assertThat(testObj.isHostIsHeadlessBot(), is(false));
    testObj.setIsHostHeadlessBot(true);
    assertThat(testObj.isHostIsHeadlessBot(), is(true));
    testObj.setIsHostHeadlessBot(false);
    assertThat(testObj.isHostIsHeadlessBot(), is(false));
  }

  @Test
  void testSetAndGetClientModelForHostBots() {
    assertThat(testObj.getClientModelForHostBots(), nullValue());
    testObj.setClientModelForHostBots(mockClientModel);
    assertThat(testObj.getClientModelForHostBots(), sameInstance(mockClientModel));
    testObj.setClientModelForHostBots(null);
    assertThat(testObj.getClientModelForHostBots(), nullValue());
  }

  @Test
  void testGetGameName() {
    this.testObjectSetMockGameData();
    assertThat(testObj.getGameName(), is(fakeGameName));
  }

  @Test
  void testGetGameRound() {
    this.testObjectSetMockGameData();
    assertThat(testObj.getGameRound(), is(fakeGameRound));
  }

  @Test
  void testGetGameVersion() {
    this.testObjectSetMockGameData();
    assertThat(testObj.getGameVersion(), is(fakeGameVersion));
  }
}
