package games.strategy.triplea.delegate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.util.IntegerMap;

public class PlaceDelegateTest extends DelegateTest {
  protected PlaceDelegate delegate;
  protected ITestDelegateBridge bridge;

  private Collection<Unit> getInfantry(final int count, final PlayerID player) {
    return gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INFANTRY).create(count, player);
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    bridge = super.getDelegateBridge(british);
    delegate = new PlaceDelegate();
    delegate.initialize("place");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  private static Collection<Unit> getUnits(final IntegerMap<UnitType> units, final PlayerID from) {
    final Iterator<UnitType> iter = units.keySet().iterator();
    final Collection<Unit> rVal = new ArrayList<>(units.totalValues());
    while (iter.hasNext()) {
      final UnitType type = iter.next();
      rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
    }
    return rVal;
  }

  @Test
  public void testValid() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response = delegate.placeUnits(getUnits(map, british), uk, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
  }

  @Test
  public void testNotCorrectUnitsValid() {
    final String response =
        delegate.placeUnits(infantry.create(3, british), uk, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertError(response);
  }

  @Test
  public void testOnlySeaInSeaZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response = delegate.canUnitsBePlaced(northSea, getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testSeaCanGoInSeaZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response = delegate.canUnitsBePlaced(northSea, getUnits(map, british), british);
    assertValid(response);
  }

  @Test
  public void testLandCanGoInLandZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response = delegate.placeUnits(getUnits(map, british), uk, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
  }

  @Test
  public void testSeaCantGoInSeaInLandZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response = delegate.canUnitsBePlaced(uk, getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testNoGoIfOpposingTroopsSea() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response = delegate.canUnitsBePlaced(northSea, getUnits(map, japanese), japanese);
    assertError(response);
  }

  @Test
  public void testNoGoIfOpposingTroopsLand() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response = delegate.canUnitsBePlaced(japan, getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testOnlyOneFactoryPlaced() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    final String response = delegate.canUnitsBePlaced(uk, getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testCantPlaceAAWhenOneAlreadyThere() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 1);
    final String response = delegate.canUnitsBePlaced(uk, getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testCantPlaceTwoAA() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 2);
    final String response = delegate.canUnitsBePlaced(westCanada, getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testProduceFactory() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    final String response = delegate.canUnitsBePlaced(egypt, getUnits(map, british), british);
    assertValid(response);
  }

  @Test
  public void testMustOwnToPlace() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response = delegate.canUnitsBePlaced(germany, getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testCanProduce() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final PlaceableUnits response = delegate.getPlaceableUnits(getUnits(map, british), westCanada);
    assertFalse(response.isError());
  }

  @Test
  public void testCanProduceInSea() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final PlaceableUnits response = delegate.getPlaceableUnits(getUnits(map, british), northSea);
    assertFalse(response.isError());
  }

  @Test
  public void testCanNotProduceThatManyUnits() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 3);
    final PlaceableUnits response = delegate.getPlaceableUnits(getUnits(map, british), westCanada);
    assertTrue(response.getMaxUnits() == 2);
  }

  @Test
  public void testAlreadyProducedUnits() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    final Map<Territory, Collection<Unit>> alreadyProduced = new HashMap<>();
    alreadyProduced.put(westCanada, getInfantry(2, british));
    delegate.setProduced(alreadyProduced);
    map.add(infantry, 1);
    final PlaceableUnits response = delegate.getPlaceableUnits(getUnits(map, british), westCanada);
    assertTrue(response.getMaxUnits() == 0);
  }

  @Test
  public void testMultipleFactories() {
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    String response = delegate.canUnitsBePlaced(egypt, getUnits(map, british), british);
    // we can place 1 factory
    assertValid(response);
    // we cant place 2
    map = new IntegerMap<>();
    map.add(factory, 2);
    response = delegate.canUnitsBePlaced(egypt, getUnits(map, british), british);
    assertError(response);
  }
}
