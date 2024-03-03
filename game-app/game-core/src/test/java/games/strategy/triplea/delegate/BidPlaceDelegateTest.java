package games.strategy.triplea.delegate;

import static games.strategy.triplea.Constants.DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES;
import static games.strategy.triplea.Constants.UNIT_PLACEMENT_RESTRICTIONS;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate.BidMode.NOT_BID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

class BidPlaceDelegateTest extends AbstractDelegateTestCase {
  private BidPlaceDelegate delegate;

  @BeforeEach
  void setupPlaceDelegate() {
    final IDelegateBridge bridge = newDelegateBridge(british);
    delegate = new BidPlaceDelegate();
    delegate.initialize("bid");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  @Test
  void testValid() {
    final String response = delegate.placeUnits(create(british, infantry, 2), uk, NOT_BID);
    assertValid(response);
  }

  @Test
  void testNotCorrectUnitsValid() {
    final var unitsNotHeldByPlayer = infantry.create(3, british);
    final String response = delegate.placeUnits(unitsNotHeldByPlayer, uk, NOT_BID);
    assertError(response);
  }

  @Test
  void testOnlySeaInSeaZone() {
    final String response =
        delegate.canUnitsBePlaced(northSea, create(british, infantry, 2), british);
    assertError(response);
  }

  @Test
  void testSeaCanGoInSeaZone() {
    final String response =
        delegate.canUnitsBePlaced(northSea, create(british, transport, 2), british);
    assertValid(response);
  }

  @Test
  void testLandCanGoInLandZone() {
    final String response = delegate.placeUnits(create(british, infantry, 2), uk, NOT_BID);
    assertValid(response);
  }

  @Test
  void testSeaCantGoInSeaInLandZone() {
    final String response = delegate.canUnitsBePlaced(uk, create(british, transport, 2), british);
    assertError(response);
  }

  @Test
  void testNoGoIfOpposingTroopsSea() {
    final String response =
        delegate.canUnitsBePlaced(northSea, create(japanese, transport, 2), japanese);
    assertError(response);
  }

  @Test
  void testNoGoIfOpposingTroopsLand() {
    final String response = delegate.canUnitsBePlaced(japan, create(british, infantry, 2), british);
    assertError(response);
  }

  @Test
  void testOnlyOneFactoryPlaced() {
    final String response = delegate.canUnitsBePlaced(uk, create(british, factory, 1), british);
    assertError(response);
  }

  @Test
  void testCantPlaceAaWhenOneAlreadyThere() {
    final String response = delegate.canUnitsBePlaced(uk, create(british, aaGun, 1), british);
    assertError(response);
  }

  @Test
  void testCantPlaceTwoAa() {
    final String response =
        delegate.canUnitsBePlaced(westCanada, create(british, aaGun, 2), british);
    assertError(response);
  }

  @Test
  void testProduceFactory() {
    final String response = delegate.canUnitsBePlaced(egypt, create(british, factory, 1), british);
    assertValid(response);
  }

  @Test
  void testMustOwnToPlace() {
    final String response =
        delegate.canUnitsBePlaced(germany, create(british, infantry, 2), british);
    assertError(response);
  }

  @Test
  void testCanProduce() {
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 2), westCanada);
    assertFalse(response.isError());
  }

  @Test
  void testCanProduceInSea() {
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, transport, 2), northSea);
    assertFalse(response.isError());
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
    final var infantry2 = unitType("infantry2", gameData);

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

  @Test
  void testMultipleFactories() {
    String response = delegate.canUnitsBePlaced(egypt, create(british, factory, 1), british);
    // we can place 1 factory
    assertValid(response);
    // we can't place 2
    response = delegate.canUnitsBePlaced(egypt, create(british, factory, 2), british);
    assertError(response);
  }

  @Test
  void testUnitAttachmentStackingLimit() {
    // we can place 4
    Collection<Unit> fourTanks = create(british, armour, 4);
    assertValid(delegate.canUnitsBePlaced(uk, fourTanks, british));

    // we can't place 5 per the unit attachment's placementLimit
    Collection<Unit> fiveTanks = create(british, armour, 5);
    assertError(delegate.canUnitsBePlaced(uk, fiveTanks, british));

    // we can't place 3, if 2 are already scheduled to be placed
    delegate.setProduced(Map.of(uk, create(british, armour, 2)));
    Collection<Unit> threeTanks = create(british, armour, 3);
    assertError(delegate.canUnitsBePlaced(uk, threeTanks, british));
    // but we can place 2
    Collection<Unit> twoTanks = create(british, armour, 2);
    assertValid(delegate.canUnitsBePlaced(uk, twoTanks, british));

    // we also can't place 3, if there's one in the territory and another scheduled to be placed.
    delegate.setProduced(Map.of(uk, create(british, armour, 1)));
    uk.getUnitCollection().addAll(create(british, armour, 1));
    assertError(delegate.canUnitsBePlaced(uk, threeTanks, british));
    // but we can place 2
    assertValid(delegate.canUnitsBePlaced(uk, twoTanks, british));
  }

  @Test
  void testPlayerAttachmentStackingLimit() {
    // we can place 3 battleships
    Collection<Unit> units = create(british, battleship, 3);
    assertValid(delegate.canUnitsBePlaced(northSea, units, british));
    // but not 4
    units = create(british, battleship, 4);
    assertError(delegate.canUnitsBePlaced(northSea, units, british));

    // we can also place 2 battleships and a carrier
    units = create(british, battleship, 2);
    units.addAll(create(british, carrier, 1));
    assertValid(delegate.canUnitsBePlaced(northSea, units, british));
    // but not 2 battleships and 2 carriers
    units.addAll(create(british, carrier, 1));
    assertError(delegate.canUnitsBePlaced(northSea, units, british));
    // However, getPlaceableUnits() should return 2 of each, since that's what's for filtering the
    // options given to the user.
    assertThat(
        delegate.getPlaceableUnits(units, northSea).getUnits(),
        containsInAnyOrder(units.toArray()));
    units.addAll(create(british, carrier, 5));
    units.addAll(create(british, battleship, 7));
    var result = delegate.getPlaceableUnits(units, northSea).getUnits();
    assertThat(result, hasSize(6));
    assertThat(CollectionUtils.getMatches(result, Matches.unitIsOfType(battleship)), hasSize(3));
    assertThat(CollectionUtils.getMatches(result, Matches.unitIsOfType(carrier)), hasSize(3));
  }

  @Test
  void testUnitTerritoryPlacementRestrictionsDoNotLimitBidPlacement() {
    // Note: battleship is marked as not placeable in "West Canada Sea Zone" on the test map.
    // This should be ignored for Bid purposes, like other placement restrictions.

    // Add a carrier to the sea zone.
    westCanadaSeaZone.getUnitCollection().addAll(create(british, carrier, 1));
    List<Unit> units = create(british, battleship, 1);
    assertValid(delegate.canUnitsBePlaced(westCanadaSeaZone, units, british));
    PlaceableUnits response = delegate.getPlaceableUnits(units, westCanadaSeaZone);
    assertThat(response.getUnits(), hasSize(1));
  }
}
