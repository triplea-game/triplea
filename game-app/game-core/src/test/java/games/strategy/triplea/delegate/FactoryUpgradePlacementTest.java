package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.placeDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a construction upgrade that consumes an existing construction of the same
 * constructionType can be placed even when the per-type cap would otherwise block it
 * (factory_upgrade consumes factory_minor). maxFactoriesPerTerr is forced to 1 so the existing
 * minor factory already fills the cap, making this a meaningful test of the consumption logic
 * rather than a no-op.
 */
class FactoryUpgradePlacementTest {
  private final GameData gameData = TestMapGameData.GLOBAL1940.getGameData();
  private final GamePlayer americans = americans(gameData);

  @Test
  void factoryUpgradeCanBePlacedWhenItConsumesExistingMinorFactory() {
    final Territory target = territory("Western United States", gameData);

    final UnitType factoryUpgrade = unitType("factory_upgrade", gameData);

    assertNotNull(factoryUpgrade, "factory_upgrade unit type must exist in map");

    gameData.getProperties().set(Constants.FACTORIES_PER_COUNTRY_PROPERTY, 1);

    // Create the held upgrade unit and add it to the player's pool
    final List<Unit> held = factoryUpgrade.create(1, americans);
    addTo(americans, held, gameData);

    final IDelegateBridge bridge = newDelegateBridge(americans);

    // The test map does not define Western United States as originally American,
    // which is required for factory_upgrade placement.
    bridge.addChange(OriginalOwnerTracker.addOriginalOwnerChange(target, americans));

    advanceToStep(bridge, "PlaceUnits");

    final PlaceDelegate placeDelegate = placeDelegate(gameData);
    placeDelegate.setDelegateBridgeAndPlayer(bridge);
    placeDelegate.start();

    final Optional<String> canPlaceError = placeDelegate.canUnitsBePlaced(target, held, americans);
    assertTrue(
        canPlaceError.isEmpty(),
        () -> "Expected placement to be allowed but got error: " + canPlaceError.get());

    final Optional<String> placeResult = placeDelegate.placeUnits(held, target);
    assertTrue(
        placeResult.isEmpty(),
        () -> "Expected placeUnits to succeed but got error: " + placeResult.get());
  }
}
