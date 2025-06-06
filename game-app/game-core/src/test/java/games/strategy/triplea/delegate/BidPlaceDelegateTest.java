package games.strategy.triplea.delegate;

import static games.strategy.triplea.Constants.DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES;
import static games.strategy.triplea.Constants.UNIT_PLACEMENT_RESTRICTIONS;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate.BidMode.BID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BidPlaceDelegateTest extends PlaceDelegateTestCommon {
  @Override
  protected void setupDelegate(GamePlayer player) {
    final IDelegateBridge bridge = newDelegateBridge(player);
    delegate = new BidPlaceDelegate();
    delegate.initialize("bid");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  @Test
  void tesCanPlaceWithoutFactory() {
    final Optional<String> response = delegate.placeUnits(create(british, infantry, 2), egypt, BID);
    assertValid(response);
  }

  @Test
  void testCanPlaceSeaWithoutFactory() {
    final Optional<String> response =
        delegate.placeUnits(create(british, transport, 2), redSea, BID);
    assertValid(response);
  }

  @Test
  void testCanProduceAboveTerritoryLimit() {
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 25), westCanada);
    assertFalse(response.isError());
    assertEquals(25, response.getMaxUnits());
  }

  @Test
  void testRequiresUnitsSeaDoesNotLimitBidPlacement() {
    gameData.getProperties().set(UNIT_PLACEMENT_RESTRICTIONS, true);
    // Needed for canProduceXUnits to work. (!)
    gameData.getProperties().set(DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES, true);
    final var sub2 = unitType("submarine2", gameData);

    final var threeSub2 = create(british, sub2, 3);
    final var fourSub2 = create(british, sub2, 4);

    uk.getUnitCollection().clear();
    northSea.getUnitCollection().clear();
    // Need to have one unit already to be able to place more during bid.
    northSea.getUnitCollection().addAll(create(british, unitType("transport", gameData), 1));
    assertValid(delegate.canUnitsBePlaced(northSea, threeSub2, british));
    assertValid(delegate.canUnitsBePlaced(northSea, fourSub2, british));
    final PlaceableUnits response = delegate.getPlaceableUnits(fourSub2, northSea);
    assertThat(response.getUnits(), hasSize(4));
    // We also can't place the subs in UK since they're sea units. :)
    assertError(delegate.canUnitsBePlaced(uk, threeSub2, british));
  }

  @Test
  void testRequiresUnitsDoesNotLimitBidPlacement() {
    gameData.getProperties().set(UNIT_PLACEMENT_RESTRICTIONS, true);
    // Needed for canProduceXUnits to work. (!)
    gameData.getProperties().set(DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES, true);

    final var threeInfantry2 = create(british, infantry2, 3);
    final var fourInfantry2 = create(british, infantry2, 4);

    uk.getUnitCollection().clear();
    assertValid(delegate.canUnitsBePlaced(uk, threeInfantry2, british));
    assertValid(delegate.canUnitsBePlaced(uk, fourInfantry2, british));
    final PlaceableUnits response = delegate.getPlaceableUnits(fourInfantry2, uk);
    assertThat(response.getUnits(), hasSize(4));
  }

  @Test
  void testAlreadyProducedUnitsIgnoredForBid() {
    delegate.setProduced(Map.of(westCanada, create(british, infantry, 2)));
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 25), westCanada);
    assertFalse(response.isError());
    assertEquals(25, response.getMaxUnits());
  }
}
