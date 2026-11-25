package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.xml.TestMapGameData;
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
