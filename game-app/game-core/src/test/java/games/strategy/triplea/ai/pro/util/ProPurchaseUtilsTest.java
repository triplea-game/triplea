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
import games.strategy.engine.delegate.IDelegate;
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
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProPurchaseUtilsTest {
  final GameData twwGameData = TestMapGameData.TWW.getGameData();
  final GamePlayer british = checkNotNull(britain(twwGameData));
  final UnitType trenchType = checkNotNull(unitType("britishEntrenchment", twwGameData));
  final UnitType materialType = checkNotNull(unitType("Material", twwGameData));
  final UnitType fortType = checkNotNull(unitType("britishFortification", twwGameData));
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
  /// The map has for Egypt 7 territories with a Flagpole unit inside allowing unit production.
  /// Therefore, the result should:
  /// 1. Contain 7 entries, but not Cairo (even though owned by Egypt it has no Flagpole unit).
  /// 2. Contain one entry for each territory owned by Egypt.
  /// 3. For each territory, the corresponding `canPlace` entries should be
  /// either the territory itself or an adjacent sea zone (51, 53 or 72).
  @Test
  void testFindPurchaseTerritoriesWithUnitCanProduceUnits() {
    final GamePlayer playerEgypt = checkNotNull(twwGameData.getPlayerList().getPlayerId("Egypt"));
    final int countTerritoriesOwnedByEgypt = 7;
    final Territory cairo = twwGameData.getMap().getTerritoryOrThrow("Cairo");
    final Territory seaZone51 = twwGameData.getMap().getTerritoryOrThrow("51 Sea Zone");
    final Territory seaZone53 = twwGameData.getMap().getTerritoryOrThrow("53 Sea Zone");
    final Territory seaZone72 = twwGameData.getMap().getTerritoryOrThrow("72 Sea Zone");
    final ProAi proAi = getProAiForPurchaseStepOfPlayer(playerEgypt, twwGameData);

    final Map<Territory, ProPurchaseTerritory> foundTerritoriesToPpt =
        ProPurchaseUtils.findPurchaseTerritories(proAi.getProData(), playerEgypt);

    Assertions.assertEquals(playerEgypt, cairo.getOwner());
    Assertions.assertEquals(countTerritoriesOwnedByEgypt, foundTerritoriesToPpt.size());
    foundTerritoriesToPpt.forEach(
        (territory, proPurchaseTerritory) -> {
          Assertions.assertTrue(Matches.isTerritoryOwnedBy(playerEgypt).test(territory));
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

  @Nonnull
  private ProAi getProAiForPurchaseStepOfPlayer(GamePlayer playerEgypt, GameData gameData) {
    final ProAi proAi = new ProAi("Test Name", "Test Player Label");
    Optional<IDelegate> delegateOptional = gameData.getDelegateOptional("Purchase");
    PurchaseDelegate purchaseDelegate =
        (PurchaseDelegate) (delegateOptional.orElseGet(() -> gameData.getDelegate("purchase")));
    final IDelegateBridge testBridge = newDelegateBridge(playerEgypt);
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    final PlayerBridge playerBridgeMock = mock(PlayerBridge.class);
    when(playerBridgeMock.getGameData()).thenReturn(gameData);
    proAi.initialize(playerBridgeMock, playerEgypt);
    proAi.initializeData();
    return proAi;
  }
}
