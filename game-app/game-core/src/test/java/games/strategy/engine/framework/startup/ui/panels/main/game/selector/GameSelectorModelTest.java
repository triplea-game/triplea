package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.Observer;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameSelectorModelTest extends AbstractClientSettingTestCase {
  @NonNls private static final String fakeGameRound = "3";
  @NonNls private static final String fakeGameName = "_fakeGameName_";

  private GameSelectorModel testObj;

  @Mock private GameData mockGameData;

  @Mock private GameSequence mockSequence;

  @Mock private Observer mockObserver;

  @Mock private ClientModel mockClientModel;

  private static void assertHasEmptyData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getGameData()).isNull();
    assertHasEmptyDisplayData(objectToCheck);
  }

  private static void assertHasEmptyDisplayData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getFileName()).isEqualTo("-");
    assertThat(objectToCheck.getGameName()).isEqualTo("-");
    assertThat(objectToCheck.getGameRound()).isEqualTo("-");
  }

  private static void assertHasFakeTestData(final GameSelectorModel objectToCheck) {
    assertThat(objectToCheck.getGameName()).isEqualTo(fakeGameName);
    assertThat(objectToCheck.getGameRound()).isEqualTo(fakeGameRound);
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
    assertThat(testObj.getGameData()).isSameAs(mockGameData);
    assertHasFakeTestData(testObj);
    this.verifyTestObjectObserverUpdateSent();
  }

  private void verifyTestObjectObserverUpdateSent() {
    verify(mockObserver, times(1)).update(Mockito.any(), Mockito.any());
    reset(mockObserver);
  }

  private void prepareMockGameDataExpectations() {
    when(mockGameData.getSequence()).thenReturn(mockSequence);
    when(mockSequence.getRound()).thenReturn(Integer.valueOf(fakeGameRound));
    when(mockGameData.getGameName()).thenReturn(fakeGameName);
  }

  @Test
  void testCanSelect() {
    assertThat(testObj.isCanSelect()).isTrue();
    testObj.setCanSelect(false);
    assertThat(testObj.isCanSelect()).isFalse();
    testObj.setCanSelect(true);
    assertThat(testObj.isCanSelect()).isTrue();
  }

  @Test
  void testClearDataButKeepGameInfo() {
    this.testObjectSetMockGameData();

    final String newGameName = " 123";
    final String newGameRound = "gameRound xyz";

    testObj.clearDataButKeepGameInfo(newGameName, newGameRound);
    verifyTestObjectObserverUpdateSent();
    assertThat(testObj.getGameData()).isNull();
    assertThat(testObj.getGameName()).isEqualTo(newGameName);
    assertThat(testObj.getGameRound()).isEqualTo(newGameRound);
  }

  @Test
  void testGetGameData() {
    assertThat(testObj.getGameData()).isNull();
    prepareMockGameDataExpectations();
    testObj.setGameData(mockGameData);
    assertThat(testObj.getGameData()).isSameAs(mockGameData);
  }

  @Test
  void testSetAndGetIsHostHeadlessBot() {
    assertThat(testObj.isHostIsHeadlessBot()).isFalse();
    testObj.setIsHostHeadlessBot(true);
    assertThat(testObj.isHostIsHeadlessBot()).isTrue();
    testObj.setIsHostHeadlessBot(false);
    assertThat(testObj.isHostIsHeadlessBot()).isFalse();
  }

  @Test
  void testSetAndGetClientModelForHostBots() {
    assertThat(testObj.getClientModelForHostBots()).isNull();
    testObj.setClientModelForHostBots(mockClientModel);
    assertThat(testObj.getClientModelForHostBots()).isSameAs(mockClientModel);
    testObj.setClientModelForHostBots(null);
    assertThat(testObj.getClientModelForHostBots()).isNull();
  }

  @Test
  void testGetGameName() {
    this.testObjectSetMockGameData();
    assertThat(testObj.getGameName()).isEqualTo(fakeGameName);
  }

  @Test
  void testGetGameRound() {
    this.testObjectSetMockGameData();
    assertThat(testObj.getGameRound()).isEqualTo(fakeGameRound);
  }
}
