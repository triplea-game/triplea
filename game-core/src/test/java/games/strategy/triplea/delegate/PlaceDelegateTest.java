package games.strategy.triplea.delegate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  @BeforeEach
  public void setupPlaceDelegate() {
    bridge = super.getDelegateBridge(british);
    delegate = new PlaceDelegate();
    delegate.initialize("place");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  @Test
  public void testValid() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response =
        delegate.placeUnits(GameDataTestUtil.getUnits(map, british), uk, IAbstractPlaceDelegate.BidMode.NOT_BID);
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
    final String response = delegate.canUnitsBePlaced(northSea, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testSeaCanGoInSeaZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response = delegate.canUnitsBePlaced(northSea, GameDataTestUtil.getUnits(map, british), british);
    assertValid(response);
  }

  @Test
  public void testLandCanGoInLandZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response =
        delegate.placeUnits(GameDataTestUtil.getUnits(map, british), uk, IAbstractPlaceDelegate.BidMode.NOT_BID);
    assertValid(response);
  }

  @Test
  public void testSeaCantGoInSeaInLandZone() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response = delegate.canUnitsBePlaced(uk, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testNoGoIfOpposingTroopsSea() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final String response = delegate.canUnitsBePlaced(northSea, GameDataTestUtil.getUnits(map, japanese), japanese);
    assertError(response);
  }

  @Test
  public void testNoGoIfOpposingTroopsLand() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response = delegate.canUnitsBePlaced(japan, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testOnlyOneFactoryPlaced() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    final String response = delegate.canUnitsBePlaced(uk, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testCantPlaceAaWhenOneAlreadyThere() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 1);
    final String response = delegate.canUnitsBePlaced(uk, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testCantPlaceTwoAa() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(aaGun, 2);
    final String response = delegate.canUnitsBePlaced(westCanada, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testProduceFactory() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    final String response = delegate.canUnitsBePlaced(egypt, GameDataTestUtil.getUnits(map, british), british);
    assertValid(response);
  }

  @Test
  public void testMustOwnToPlace() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final String response = delegate.canUnitsBePlaced(germany, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }

  @Test
  public void testCanProduce() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 2);
    final PlaceableUnits response = delegate.getPlaceableUnits(GameDataTestUtil.getUnits(map, british), westCanada);
    assertFalse(response.isError());
  }

  @Test
  public void testCanProduceInSea() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(transport, 2);
    final PlaceableUnits response = delegate.getPlaceableUnits(GameDataTestUtil.getUnits(map, british), northSea);
    assertFalse(response.isError());
  }

  @Test
  public void testCanNotProduceThatManyUnits() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(infantry, 3);
    final PlaceableUnits response = delegate.getPlaceableUnits(GameDataTestUtil.getUnits(map, british), westCanada);
    assertTrue(response.getMaxUnits() == 2);
  }

  @Test
  public void testAlreadyProducedUnits() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    final Map<Territory, Collection<Unit>> alreadyProduced = new HashMap<>();
    alreadyProduced.put(westCanada, getInfantry(2, british));
    delegate.setProduced(alreadyProduced);
    map.add(infantry, 1);
    final PlaceableUnits response = delegate.getPlaceableUnits(GameDataTestUtil.getUnits(map, british), westCanada);
    assertTrue(response.getMaxUnits() == 0);
  }

  @Test
  public void testMultipleFactories() {
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.add(factory, 1);
    String response = delegate.canUnitsBePlaced(egypt, GameDataTestUtil.getUnits(map, british), british);
    // we can place 1 factory
    assertValid(response);
    // we cant place 2
    map = new IntegerMap<>();
    map.add(factory, 2);
    response = delegate.canUnitsBePlaced(egypt, GameDataTestUtil.getUnits(map, british), british);
    assertError(response);
  }
}
