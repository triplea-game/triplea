package games.strategy.triplea.delegate;

import static games.strategy.triplea.Constants.DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES;
import static games.strategy.triplea.Constants.UNIT_PLACEMENT_RESTRICTIONS;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate.BidMode.NOT_BID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import java.util.Map;
import org.junit.jupiter.api.Test;

// Note: This inherits from PlaceDelegateTestCommon and the tests defined there are included.
class PlaceDelegateTest extends PlaceDelegateTestCommon {
  @Override
  protected void setupDelegate(GamePlayer player) {
    final IDelegateBridge bridge = newDelegateBridge(player);
    delegate = new PlaceDelegate();
    delegate.initialize("place");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  @Test
  void testCannotPlaceWithoutFactory() {
    final String response = delegate.placeUnits(create(british, infantry, 2), egypt, NOT_BID);
    assertError(response);
  }

  @Test
  void testCannotPlaceSeaWithoutFactory() {
    final String response = delegate.placeUnits(create(british, transport, 2), redSea, NOT_BID);
    assertError(response);
  }

  @Test
  void testCannotProduceThatManyUnits() {
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 3), westCanada);
    assertEquals(2, response.getMaxUnits());
  }

  @Test
  void testCannotProduceThatManyUnitsDueToRequiresUnits() {
    gameData.getProperties().set(UNIT_PLACEMENT_RESTRICTIONS, true);
    // Needed for canProduceXUnits to work. (!)
    gameData.getProperties().set(DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES, true);

    final var threeInfantry2 = create(british, infantry2, 3);
    final var fourInfantry2 = create(british, infantry2, 4);

    uk.getUnitCollection().clear();
    assertError(delegate.canUnitsBePlaced(uk, threeInfantry2, british));
    uk.getUnitCollection().addAll(create(british, factory2, 1));
    assertValid(delegate.canUnitsBePlaced(uk, threeInfantry2, british));
    assertError(delegate.canUnitsBePlaced(uk, fourInfantry2, british));
    final PlaceableUnits response = delegate.getPlaceableUnits(fourInfantry2, uk);
    assertThat(response.getUnits(), hasSize(3));
  }

  @Test
  void testRequiresUnitsSea() {
    gameData.getProperties().set(UNIT_PLACEMENT_RESTRICTIONS, true);
    // Needed for canProduceXUnits to work. (!)
    gameData.getProperties().set(DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES, true);
    final var sub2 = unitType("submarine2", gameData);

    final var threeSub2 = create(british, sub2, 3);
    final var fourSub2 = create(british, sub2, 4);

    uk.getUnitCollection().clear();
    northSea.getUnitCollection().clear();
    assertError(delegate.canUnitsBePlaced(northSea, threeSub2, british));
    uk.getUnitCollection().addAll(create(british, factory2, 1));
    assertValid(delegate.canUnitsBePlaced(northSea, threeSub2, british));
    assertError(delegate.canUnitsBePlaced(northSea, fourSub2, british));
    final PlaceableUnits response = delegate.getPlaceableUnits(fourSub2, northSea);
    assertThat(response.getUnits(), hasSize(3));
    // We also can't place the subs in UK since they're sea units. :)
    assertError(delegate.canUnitsBePlaced(uk, threeSub2, british));
  }

  @Test
  void testAlreadyProducedUnits() {
    delegate.setProduced(Map.of(westCanada, create(british, infantry, 2)));
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 1), westCanada);
    assertEquals(0, response.getMaxUnits());
  }
}
