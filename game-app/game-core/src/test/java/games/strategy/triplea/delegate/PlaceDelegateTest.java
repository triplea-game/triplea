package games.strategy.triplea.delegate;

import static games.strategy.triplea.Constants.DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES;
import static games.strategy.triplea.Constants.UNIT_PLACEMENT_RESTRICTIONS;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static games.strategy.triplea.delegate.Matches.unitIsOfType;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate.BidMode.NOT_BID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

class PlaceDelegateTest extends AbstractDelegateTestCase {
  private PlaceDelegate delegate;

  @BeforeEach
  void setupPlaceDelegate() {
    final IDelegateBridge bridge = newDelegateBridge(british);
    delegate = new PlaceDelegate();
    delegate.initialize("place");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  private List<Unit> create(GamePlayer player, UnitType unitType, int quantity) {
    var units = unitType.create(quantity, player);
    player.getUnitCollection().addAll(units);
    return units;
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
  void testCanNotProduceThatManyUnits() {
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 3), westCanada);
    assertEquals(2, response.getMaxUnits());
  }

  @Test
  void testCanNotProduceThatManyUnitsDueToRequiresUnits() {
    gameData.getProperties().set(UNIT_PLACEMENT_RESTRICTIONS, true);
    // Needed for canProduceXUnits to work. (!)
    gameData.getProperties().set(DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES, true);
    final var factory2 = unitType("factory2", gameData);
    final var infantry2 = unitType("infantry2", gameData);

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
  void testAlreadyProducedUnits() {
    delegate.setProduced(Map.of(westCanada, create(british, infantry, 2)));
    final PlaceableUnits response =
        delegate.getPlaceableUnits(create(british, infantry, 1), westCanada);
    assertEquals(0, response.getMaxUnits());
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
  void testStackingLimitFilteringHappensAfterPlacementRestrictions() {
    // Note: battleship is marked as not placeable in "West Canada Sea Zone" on the test map.

    // Add a carrier to the sea zone.
    westCanadaSeaZone.getUnitCollection().addAll(create(british, carrier, 1));

    // If we filter list of 2 battleships and 2 carriers, the 2 carriers should be selected.
    List<Unit> units = create(british, battleship, 2);
    units.addAll(create(british, carrier, 2));
    // First, we can't place all of them (expected).
    assertError(delegate.canUnitsBePlaced(westCanadaSeaZone, units, british));

    PlaceableUnits response = delegate.getPlaceableUnits(units, westCanadaSeaZone);
    assertThat(response.getUnits(), hasSize(2));
    assertThat(response.getUnits(), is(CollectionUtils.getMatches(units, unitIsOfType(carrier))));

    // Check that it's the case even if we shuffle the list a few times.
    for (int i = 0; i < 5; i++) {
      Collections.shuffle(units);
      response = delegate.getPlaceableUnits(units, westCanadaSeaZone);
      assertThat(response.getUnits(), hasSize(2));
      assertThat(response.getUnits(), is(CollectionUtils.getMatches(units, unitIsOfType(carrier))));
    }
  }
}
