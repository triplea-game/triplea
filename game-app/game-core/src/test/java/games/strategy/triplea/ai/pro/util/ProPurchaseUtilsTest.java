package games.strategy.triplea.ai.pro.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.britain;
import static games.strategy.triplea.delegate.GameDataTestUtil.italy;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
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
    assertThat(
        getUnitsToConsume(List.of(trench1, material1), List.of(fort1)),
        containsInAnyOrder(trench1, material1));
  }

  @Test
  void getUnitsToConsumeTwoUnits() {
    assertThat(
        getUnitsToConsume(List.of(trench1, trench2, material1, material2), List.of(fort1, fort2)),
        containsInAnyOrder(trench1, trench2, material1, material2));
  }

  @Test
  void getUnitsToConsumeMultipleTypes() {
    // Trench requires 1 material + fort requires 1 trench and 1 material.
    assertThat(
        getUnitsToConsume(List.of(fort2, material1, material2, trench2), List.of(trench1, fort1)),
        containsInAnyOrder(material1, material2, trench2));
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

  @Test
  void testFindPurchaseTerritories() {
    final GamePlayer italy = checkNotNull(italy(gameData));

    final ProAi proAi = new ProAi("Test Name", "Test Player Label");
    PurchaseDelegate purchaseDelegate = (PurchaseDelegate) gameData.getDelegate("Purchase");
    final IDelegateBridge testBridge = newDelegateBridge(italy);
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    final PlayerBridge playerBridgeMock = mock(PlayerBridge.class);
    when(playerBridgeMock.getGameData()).thenReturn(gameData);
    proAi.initialize(playerBridgeMock, italy);
    proAi.initializeData();

    final Map<Territory, ProPurchaseTerritory> foundTerritoriesToPpt =
        ProPurchaseUtils.findPurchaseTerritories(proAi.getProData(), italy);

    // specific test-case generically checked: Should return (1) 11 entries,
    // (2) each having 1 or more canPlace-entries and
    // (3) each canPlace-entries is the territory itself or an adjacent sea zone
    assertThat(foundTerritoriesToPpt.size(), equalTo(11));
    foundTerritoriesToPpt.forEach(
        (territory, ppt) -> {
          assertThat(Matches.isTerritoryOwnedBy(italy).test(territory), equalTo(true));
          List<ProPlaceTerritory> canPlacePpts = ppt.getCanPlaceTerritories();
          assertThat(canPlacePpts.isEmpty(), equalTo(false));
          if (!territory.getName().equals("Al Kufrah")) {
            assertThat(canPlacePpts.size() > 1, equalTo(true));
          }
          Set<Territory> adjacentSeaZones =
              gameData.getMap().getNeighbors(territory, Matches.territoryIsWater());
          canPlacePpts.forEach(
              canPlacePpt -> {
                Territory canPlaceTerritory = canPlacePpt.getTerritory();
                assertThat(
                    canPlaceTerritory.equals(territory)
                        || adjacentSeaZones.contains(canPlaceTerritory),
                    equalTo(true));
              });
        });
  }
}
