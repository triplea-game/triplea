package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.carrier;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

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

  @Test
  void testValid() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response =
        delegate.placeUnits(
            GameDataTestUtil.getUnits(map, british), uk, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
  }

  @Test
  void testNotCorrectUnitsValid() {
    final String response =
        delegate.placeUnits(
            infantry.create(3, british), uk, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertError(response);
  }

  @Test
  void testOnlySeaInSeaZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response =
        delegate.canUnitsBePlaced(northSea, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  void testSeaCanGoInSeaZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response =
        delegate.canUnitsBePlaced(northSea, GameDataTestUtil.getUnits(map, british), british);
    assertValid(response);
  }

  @Test
  void testLandCanGoInLandZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response =
        delegate.placeUnits(
            GameDataTestUtil.getUnits(map, british), uk, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
  }

  @Test
  void testSeaCantGoInSeaInLandZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response =
        delegate.canUnitsBePlaced(uk, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  void testNoGoIfOpposingTroopsSea() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response =
        delegate.canUnitsBePlaced(northSea, GameDataTestUtil.getUnits(map, japanese), japanese);
    assertError(response);
  }

  @Test
  void testNoGoIfOpposingTroopsLand() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response =
        delegate.canUnitsBePlaced(japan, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  void testOnlyOneFactoryPlaced() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    final String response =
        delegate.canUnitsBePlaced(uk, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  void testCantPlaceAaWhenOneAlreadyThere() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 1);
    final String response =
        delegate.canUnitsBePlaced(uk, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  void testCantPlaceTwoAa() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 2);
    final String response =
        delegate.canUnitsBePlaced(westCanada, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  void testProduceFactory() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    final String response =
        delegate.canUnitsBePlaced(egypt, GameDataTestUtil.getUnits(map, british), british);
    assertValid(response);
  }

  @Test
  void testMustOwnToPlace() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response =
        delegate.canUnitsBePlaced(germany, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  void testCanProduce() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final PlaceableUnits response =
        delegate.getPlaceableUnits(GameDataTestUtil.getUnits(map, british), westCanada);
    assertFalse(response.isError());
  }

  @Test
  void testCanProduceInSea() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final PlaceableUnits response =
        delegate.getPlaceableUnits(GameDataTestUtil.getUnits(map, british), northSea);
    assertFalse(response.isError());
  }

  @Test
  void testCanNotProduceThatManyUnits() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 3);
    final PlaceableUnits response =
        delegate.getPlaceableUnits(GameDataTestUtil.getUnits(map, british), westCanada);
    assertEquals(2, response.getMaxUnits());
  }

  @Test
  void testAlreadyProducedUnits() {
    delegate.setProduced(Map.of(westCanada, infantry(gameData).create(2, british)));
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 1);
    final PlaceableUnits response =
        delegate.getPlaceableUnits(GameDataTestUtil.getUnits(map, british), westCanada);
    assertEquals(0, response.getMaxUnits());
  }

  @Test
  void testMultipleFactories() {
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    String response =
        delegate.canUnitsBePlaced(egypt, GameDataTestUtil.getUnits(map, british), british);
    // we can place 1 factory
    assertValid(response);
    // we can't place 2
    map = new IntegerMap<>();
    map.add(factory, 2);
    response = delegate.canUnitsBePlaced(egypt, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  void testUnitAttachmentStackingLimit() {
    // we can place 4
    Collection<Unit> fourTanks = armour(gameData).create(4, british);
    assertValid(delegate.canUnitsBePlaced(uk, fourTanks, british));

    // we can't place 5 per the unit attachment's placementLimit
    Collection<Unit> fiveTanks = armour(gameData).create(5, british);
    assertError(delegate.canUnitsBePlaced(uk, fiveTanks, british));

    // we can't place 3, if 2 are already scheduled to be placed
    delegate.setProduced(Map.of(uk, armour(gameData).create(2, british)));
    Collection<Unit> threeTanks = armour(gameData).create(5, british);
    assertError(delegate.canUnitsBePlaced(uk, threeTanks, british));
    // but we can place 2
    Collection<Unit> twoTanks = armour(gameData).create(2, british);
    assertValid(delegate.canUnitsBePlaced(uk, twoTanks, british));

    // we also can't place 3, if there's one in the territory and another scheduled to be placed.
    delegate.setProduced(Map.of(uk, armour(gameData).create(1, british)));
    uk.getUnitCollection().addAll(armour(gameData).create(1, british));
    assertError(delegate.canUnitsBePlaced(uk, threeTanks, british));
    // but we can place 2
    assertValid(delegate.canUnitsBePlaced(uk, twoTanks, british));
  }

  @Test
  void testPlayerAttachmentStackingLimit() {
    // we can place 3 battleships
    Collection<Unit> units = battleship(gameData).create(3, british);
    assertValid(delegate.canUnitsBePlaced(northSea, units, british));
    // but not 4
    units = battleship(gameData).create(4, british);
    assertError(delegate.canUnitsBePlaced(northSea, units, british));

    // we can also place 2 battleships and a carrier
    units = battleship(gameData).create(2, british);
    units.addAll(carrier(gameData).create(1, british));
    assertValid(delegate.canUnitsBePlaced(northSea, units, british));
    // but not 2 battleships and 2 carriers
    units.addAll(carrier(gameData).create(1, british));
    assertError(delegate.canUnitsBePlaced(northSea, units, british));
  }
}
