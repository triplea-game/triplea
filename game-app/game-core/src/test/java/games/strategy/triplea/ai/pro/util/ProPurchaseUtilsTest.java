package games.strategy.triplea.ai.pro.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.britain;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.pro.data.ProPlaceTerritory;
import games.strategy.triplea.ai.pro.data.ProPurchaseTerritory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PurchaseDelegate;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProPurchaseUtilsTest {
  final GameData gameData = TestMapGameData.TWW.getGameData();
  final GamePlayer british = checkNotNull(britain(gameData));
  final UnitType trenchType = checkNotNull(unitType("britishEntrenchment", gameData));
  final UnitType materialType = checkNotNull(unitType("Material", gameData));
  final UnitType fortType = checkNotNull(unitType("britishFortification", gameData));
  final Unit trench1 = trenchType.create(british);
  final Unit trench2 = trenchType.create(british);
  final Unit material1 = materialType.create(british);
  final Unit material2 = materialType.create(british);
  final Unit fort1 = fortType.create(british);
  final Unit fort2 = fortType.create(british);

  @Test
  void getUnitsToConsumeBasic() {
    // Fort requires 1 trench and 1 material.
    assertThatContainsInAnyOrder(
        Set.of(trench1, material1), getUnitsToConsume(List.of(trench1, material1), List.of(fort1)));
  }

  private void assertThatContainsInAnyOrder(Set<Unit> expectedUnits, Collection<Unit> actual) {
    Assertions.assertEquals(expectedUnits, Set.copyOf(actual));
  }

  @Test
  void getUnitsToConsumeTwoUnits() {
    assertThatContainsInAnyOrder(
        Set.of(trench1, trench2, material1, material2),
        getUnitsToConsume(List.of(trench1, trench2, material1, material2), List.of(fort1, fort2)));
  }

  @Test
  void getUnitsToConsumeMultipleTypes() {
    // Trench requires 1 material + fort requires 1 trench and 1 material.
    assertThatContainsInAnyOrder(
        Set.of(material1, material2, trench2),
        getUnitsToConsume(List.of(fort2, material1, material2, trench2), List.of(trench1, fort1)));
  }

  @Test
  void getUnitsToConsumeNotEnough() {
    // An exception should be thrown if insufficient units.
    assertThrows(
        IllegalStateException.class,
        () -> getUnitsToConsume(List.of(material1, material2), List.of(fort1)));
  }

  private Collection<Unit> getUnitsToConsume(Collection<Unit> existing, Collection<Unit> toBuild) {
    return ProPurchaseUtils.getUnitsToConsume(british, existing, toBuild);
  }

  /// Test strategy:
  /// The map has property "Place in Any Territory" being set and the specific test-case is done for
  /// player Egypt.
  /// Therefore, the result should:
  /// 1. Contain 7 entries.
  /// 2. Contain one entry for each territory owned by Egypt.
  /// 3. For each territory, the corresponding `canPlace` entries should be
  /// either the territory itself or an adjacent sea zone (51, 53 or 72).
  @Test
  void testFindPurchaseTerritoriesWithPlaceInAnyTerritory() {
    final GamePlayer egypt = checkNotNull(gameData.getPlayerList().getPlayerId("Egypt"));
    final int countTerritoriesOwnedByEgypt = 7;
    final Territory seaZone51 = gameData.getMap().getTerritoryOrThrow("51 Sea Zone");
    final Territory seaZone53 = gameData.getMap().getTerritoryOrThrow("53 Sea Zone");
    final Territory seaZone72 = gameData.getMap().getTerritoryOrThrow("72 Sea Zone");

    final ProAi proAi = new ProAi("Test Name", "Test Player Label");
    PurchaseDelegate purchaseDelegate = (PurchaseDelegate) gameData.getDelegate("Purchase");
    final IDelegateBridge testBridge = newDelegateBridge(egypt);
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    final PlayerBridge playerBridgeMock = mock(PlayerBridge.class);
    when(playerBridgeMock.getGameData()).thenReturn(gameData);
    proAi.initialize(playerBridgeMock, egypt);
    proAi.initializeData();

    final Map<Territory, ProPurchaseTerritory> foundTerritoriesToPpt =
        ProPurchaseUtils.findPurchaseTerritories(proAi.getProData(), egypt);

    Assertions.assertEquals(countTerritoriesOwnedByEgypt, foundTerritoriesToPpt.size());
    foundTerritoriesToPpt.forEach(
        (territory, proPurchaseTerritory) -> {
          Assertions.assertTrue(Matches.isTerritoryOwnedBy(egypt).test(territory));
          List<ProPlaceTerritory> canPlacePlaceTerritories =
              proPurchaseTerritory.getCanPlaceTerritories();
          Assertions.assertFalse(canPlacePlaceTerritories.isEmpty());
          canPlacePlaceTerritories.forEach(
              proPlaceTerritory -> {
                Territory territoryProPlace = proPlaceTerritory.getTerritory();
                Assertions.assertTrue(
                    territoryProPlace.equals(territory)
                        || territoryProPlace.equals(seaZone51)
                        || territoryProPlace.equals(seaZone53)
                        || territoryProPlace.equals(seaZone72));
              });
        });
  }
}
