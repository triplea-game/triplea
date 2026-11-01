package games.strategy.triplea.ai.pro;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.chinese;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.delegate.PlaceDelegate;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

public class ProPurchaseAiTest {
  private ProAi proAi;
  private GameData gameData;
  private PurchaseDelegate purchaseDelegate;
  private GamePlayer gamePlayer;

  @Test
  public void testShouldSaveUpForAFleet() {
    setupShouldSaveUpForAFleetTest();

    /* Issue 11093: (https://github.com/triplea-game/triplea/issues/11093)
      This issue happens during execution of ProPurchaseAi.shouldSaveUpForAFleet when the IntegerMap<Resource> maxShipCost
      is not reassigned while looping over seaDefenseOptions and an UnsupportedOperationException is thrown later when
      multiplyAllValuesBy is called by the map. This is because the underlying implementation used by maxShipCost is the
      immutable one from the original initialization call to IntegerMap.of(), since it was not reassigned in the loop. To
      remediate this issue the original initialization of the maxShipCost IntegerMap should be done by calling the
      IntegerMap constructor which will initialize the map to be a mutable LinkedHashMap implementation. This way the
      underlying implementation will always be a mutable Map implementation, whether maxShipCost is reassigned or not.
    */
    Assertions.assertDoesNotThrow(
        () -> proAi.purchase(false, 20, purchaseDelegate, gameData, gamePlayer));
  }

  /// Pact of Steel 2 Chinese gain `placementAnySeaZone` once an ally holds Japan, letting them
  /// place ships next to any land they own; Kwangtung has no factory.
  @Test
  void placePutsShipsNextToLandWithoutFactoryUnderPlacementAnySeaZone() throws Exception {
    final GameData pos2GameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
    final GamePlayer chinese = checkNotNull(chinese(pos2GameData));
    chinese.getRulesAttachment().getPropertyOrThrow("placementAnySeaZone").setValue(true);
    final Territory kwangtung = territory("Kwangtung", pos2GameData);
    removeFrom(kwangtung, kwangtung.getUnits());
    pos2GameData.performChange(ChangeFactory.changeOwner(kwangtung, chinese));
    final List<Unit> transports = transport(pos2GameData).create(2, chinese);
    pos2GameData.performChange(ChangeFactory.addUnits(chinese, transports));
    final IDelegateBridge bridge = newDelegateBridge(chinese);
    advanceToStep(bridge, "chinesePlace");
    final PlaceDelegate placeDelegate = new PlaceDelegate();
    placeDelegate.initialize("place");
    placeDelegate.setDelegateBridgeAndPlayer(bridge);
    placeDelegate.start();
    final PlayerBridge playerBridgeMock = Mockito.mock(PlayerBridge.class);
    when(playerBridgeMock.getGameData()).thenReturn(pos2GameData);
    ClientSetting.setPreferences(new MemoryPreferences());
    ClientSetting.aiMovePauseDuration.setValue(0);
    final ProAi chineseAi = new ProAi("Test Name", "Test Player Label");
    chineseAi.initialize(playerBridgeMock, chinese);

    chineseAi.place(false, placeDelegate, pos2GameData, chinese);

    assertThat(territory("59 Sea Zone", pos2GameData).getUnits()).containsAll(transports);
  }

  private void setupShouldSaveUpForAFleetTest() {
    proAi = new ProAi("Test Name", "Test Player Label");
    gameData = TestMapGameData.VICTORY_TEST_SHOULD_SAVE_UP_FOR_A_FLEET.getGameData();
    gamePlayer = gameData.getPlayerList().getPlayerId("Italians");
    final IDelegateBridge testBridge = newDelegateBridge(gamePlayer);
    purchaseDelegate = (PurchaseDelegate) gameData.getDelegate("purchase");
    final PlayerBridge playerBridgeMock = Mockito.mock(PlayerBridge.class);

    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    proAi.initialize(playerBridgeMock, gamePlayer);
    ClientSetting.setPreferences(new MemoryPreferences());

    when(playerBridgeMock.getGameData()).thenReturn(gameData);
  }
}
