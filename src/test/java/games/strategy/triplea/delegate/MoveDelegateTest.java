package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

public class MoveDelegateTest extends DelegateTest {
  MoveDelegate delegate;
  ITestDelegateBridge bridge;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    bridge = super.getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegateList().getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    delegate = new MoveDelegate();
    delegate.initialize("MoveDelegate", "MoveDelegate");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  private static Collection<Unit> getUnits(final IntegerMap<UnitType> units, final Territory from) {
    final Iterator<UnitType> iter = units.keySet().iterator();
    final Collection<Unit> rVal = new ArrayList<>(units.totalValues());
    while (iter.hasNext()) {
      final UnitType type = iter.next();
      rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
    }
    return rVal;
  }

  @Test
  public void testNotUnique() {
    final Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    final List<Unit> units = armour.create(1, british);
    units.addAll(units);
    final String results = delegate.move(units, route);
    assertError(results);
  }

  @Test
  public void testNotEnoughUnits() {
    final Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    final String results = delegate.move(armour.create(10, british), route);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());
    assertError(results);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());
  }

  @Test
  public void testCantMoveEnemy() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 1);
    final Route route = new Route();
    route.setStart(algeria);
    route.add(libya);
    assertEquals(1, algeria.getUnits().size());
    assertEquals(0, libya.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    assertEquals(1, algeria.getUnits().size());
    assertEquals(0, libya.getUnits().size());
  }

  @Test
  public void testSimpleMove() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(16, egypt.getUnits().size());
    assertEquals(4, eastAfrica.getUnits().size());
  }

  @Test
  public void testSimpleMoveLength2() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final Route route = new Route();
    route.setStart(egypt);
    route.add(eastAfrica);
    route.add(kenya);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, kenya.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(16, egypt.getUnits().size());
    assertEquals(2, kenya.getUnits().size());
  }

  @Test
  public void testCanReturnToCarrier() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 3);
    final Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(antarticSea);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testLandOnCarrier() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 2);
    final Route route = new Route();
    route.setStart(egypt);
    // extra movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(mozambiqueSeaZone);
    route.add(redSea);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(16, egypt.getUnits().size());
    assertEquals(6, redSea.getUnits().size());
  }

  @Test
  public void testCantLandWithNoCarrier() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 2);
    final Route route = new Route();
    route.setStart(egypt);
    // extra movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(redSea);
    // no carriers
    route.add(mozambiqueSeaZone);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
  }

  @Test
  public void testNotEnoughCarrierCapacity() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 5);
    final Route route = new Route();
    route.setStart(egypt);
    // exast movement to force landing
    route.add(eastAfrica);
    route.add(kenya);
    route.add(mozambiqueSeaZone);
    route.add(redSea);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
  }

  @Test
  public void testLandMoveToWaterWithNoTransports() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final Route route = new Route();
    route.setStart(egypt);
    // exast movement to force landing
    route.add(eastMediteranean);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, eastMediteranean.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(0, eastMediteranean.getUnits().size());
  }

  @Test
  public void testSeaMove() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(carrier, 2);
    final Route route = new Route();
    route.setStart(redSea);
    // exast movement to force landing
    route.add(mozambiqueSeaZone);
    assertEquals(4, redSea.getUnits().size());
    assertEquals(0, mozambiqueSeaZone.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(2, redSea.getUnits().size());
    assertEquals(2, mozambiqueSeaZone.getUnits().size());
  }

  @Test
  public void testSeaCantMoveToLand() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(carrier, 2);
    final Route route = new Route();
    route.setStart(redSea);
    // exast movement to force landing
    route.add(egypt);
    assertEquals(4, redSea.getUnits().size());
    assertEquals(18, egypt.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    assertEquals(4, redSea.getUnits().size());
    assertEquals(18, egypt.getUnits().size());
  }

  @Test
  public void testLandMoveToWaterWithTransportsFull() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 1);
    map.put(infantry, 2);
    final Route route = new Route();
    route.setStart(equatorialAfrica);
    // exast movement to force landing
    route.add(congoSeaZone);
    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(11, congoSeaZone.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(11, congoSeaZone.getUnits().size());
  }

  @Test
  public void testAirCanFlyOverWater() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(bomber, 2);
    final Route route = new Route();
    route.setStart(egypt);
    // exast movement to force landing
    route.add(redSea);
    route.add(syria);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testLandMoveToWaterWithTransportsEmpty() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final Route route = new Route();
    route.setStart(egypt);
    // exast movement to force landing
    route.add(redSea);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, redSea.getUnits().size());
    final String results =
        delegate.move(getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);
    assertEquals(16, egypt.getUnits().size());
    assertEquals(6, redSea.getUnits().size());
  }

  @Test
  public void testBlitzWithArmour() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(1, algeria.getUnits().size());
    assertEquals(libya.getOwner(), japanese);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(16, egypt.getUnits().size());
    assertEquals(3, algeria.getUnits().size());
    assertEquals(libya.getOwner(), british);
  }

  @Test
  public void testCant2StepBlitzWithNonBlitzingUnits() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 1);
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    // Disable canBlitz attachment
    gameData.performChange(ChangeFactory.attachmentPropertyChange(UnitAttachment.get(armour), "false", "canBlitz"));
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Validate move happened
    assertEquals(1, libya.getUnits().size());
    assertEquals(libya.getOwner(), british);
    // Try to move 2nd space
    route = new Route();
    route.setStart(libya);
    route.add(algeria);
    // Fail because not 'canBlitz'
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testCantBlitzNuetral() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    route.add(algeria);
    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(1, algeria.getUnits().size());
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(1, algeria.getUnits().size());
  }

  @Test
  public void testOverrunNeutral() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(0, westAfrica.getUnits().size());
    assertEquals(westAfrica.getOwner(), PlayerID.NULL_PLAYERID);
    assertEquals(35, british.getResources().getQuantity(pus));
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(2, equatorialAfrica.getUnits().size());
    assertEquals(2, westAfrica.getUnits().size());
    assertEquals(westAfrica.getOwner(), british);
    assertEquals(32, british.getResources().getQuantity(pus));
  }

  @Test
  public void testAirCanOverFlyEnemy() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(bomber, 2);
    final Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);
    route.add(equatorialAfrica);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testOverrunNeutralMustStop() {
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    map = new IntegerMap<>();
    map.put(armour, 2);
    route = new Route();
    route.setStart(westAfrica);
    route.add(equatorialAfrica);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testmultipleMovesExceedingMovementLimit() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(eastAfrica);
    route.add(kenya);
    assertEquals(2, eastAfrica.getUnits().size());
    assertEquals(0, kenya.getUnits().size());
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(0, eastAfrica.getUnits().size());
    assertEquals(2, kenya.getUnits().size());
    route = new Route();
    route.setStart(kenya);
    route.add(egypt);
    assertEquals(2, kenya.getUnits().size());
    assertEquals(18, egypt.getUnits().size());
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    assertEquals(2, kenya.getUnits().size());
    assertEquals(18, egypt.getUnits().size());
  }

  @Test
  public void testMovingUnitsWithMostMovement() {
    // move 2 tanks to equatorial africa
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(equatorialAfrica);
    assertEquals(18, egypt.getUnits().size());
    assertEquals(4, equatorialAfrica.getUnits().size());
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(16, egypt.getUnits().size());
    assertEquals(6, equatorialAfrica.getUnits().size());
    // now move 2 tanks out of equatorial africa to east africa
    // only the tanks with movement 2 can make it,
    // this makes sure that the correct units are moving
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(egypt);
    route.add(eastAfrica);
    assertEquals(6, equatorialAfrica.getUnits().size());
    assertEquals(2, eastAfrica.getUnits().size());
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertEquals(4, equatorialAfrica.getUnits().size());
    assertEquals(4, eastAfrica.getUnits().size());
  }

  @Test
  public void testTransportsMustStayWithUnits() {
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);
    String results = delegate.move(getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);
    map = new IntegerMap<>();
    map.put(transport, 2);
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testUnitsStayWithTransports() {
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);
    String results = delegate.move(getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);
    map = new IntegerMap<>();
    map.put(armour, 2);
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testUnload() {
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 2);
    final Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testTransportCantLoadUnloadAfterBattle() {
    bridge = super.getDelegateBridge(russians);
    bridge.setStepName("russianCombatMove");
    westEurope.setOwner(russians);
    // Attacking force
    final List<Unit> attackTrns = transport.create(1, russians);
    final List<Unit> attackList = bomber.create(2, russians);
    attackList.addAll(attackTrns);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final DiceRoll roll = DiceRoll.rollDice(attackList, false, russians, bridge, new MockBattle(balticSeaZone), "",
        TerritoryEffectHelper.getEffects(balticSeaZone), null);
    assertEquals(2, roll.getHits());
    bridge.setStepName("russianNonCombatMove");
    // Test the move
    final Collection<Unit> moveInf = infantry.create(2, russians);
    final Route route = new Route();
    route.setStart(karelia);
    route.add(balticSeaZone);
    route.add(westEurope);
    // Once loaded, shouldnt be able to unload
    final String results = delegate.move(moveInf, route);
    assertError(results);
  }

  @Test
  public void testLoadUnloadLoadMoveTransports() {
    bridge = super.getDelegateBridge(japanese);
    bridge.setStepName("japaneseCombatMove");
    bridge.setPlayerId(japanese);
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    // Set up the test
    removeFrom(manchuria, manchuria.getUnits().getUnits());
    manchuria.setOwner(russians);
    removeFrom(japanSeaZone, japanSeaZone.getUnits().getUnits());
    gameData.performChange(ChangeFactory.addUnits(japanSeaZone, transport.create(3, japanese)));
    gameData.performChange(ChangeFactory.addUnits(japan, infantry.create(3, japanese)));
    // Perform the first load
    final Route load = new Route();
    load.setStart(japan);
    load.add(japanSeaZone);
    String results = delegate.move(Match.getNMatches(japan.getUnits().getUnits(), 1, Matches.unitIsOfType(infantry)),
        load, Match.getMatches(japanSeaZone.getUnits().getUnits(), Matches.unitIsOfType(transport)));
    assertNull(results);
    // Perform the first unload
    final Route unload = new Route();
    unload.setStart(japanSeaZone);
    unload.add(manchuria);
    results = delegate.move(Match.getNMatches(japanSeaZone.getUnits().getUnits(), 1, Matches.unitIsOfType(infantry)),
        unload);
    assertNull(results);
    // Load another trn
    final Route route2 = new Route();
    route2.setStart(japan);
    route2.add(japanSeaZone);
    results = delegate.move(Match.getNMatches(japan.getUnits().getUnits(), 1, Matches.unitIsOfType(infantry)), route2,
        Match.getMatches(japanSeaZone.getUnits().getUnits(), Matches.unitIsOfType(transport)));
    assertNull(results);
    // Move remaining units
    final Route route3 = new Route();
    route3.setStart(japanSeaZone);
    route3.add(sfeSeaZone);
    final Collection<Unit> remainingTrns = Match.getMatches(japanSeaZone.getUnits().getUnits(),
        Match.all(Matches.unitHasNotMoved, Matches.UnitWasNotLoadedThisTurn));
    results = delegate.move(remainingTrns, route3);
    assertNull(results);
  }

  @Test
  public void testUnloadedCantMove() {
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 2);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    map = new IntegerMap<>();
    // only 2 originially, would have to move the 2 we just unloaded
    // as well
    map.put(infantry, 4);
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(egypt);
    // units were unloaded, shouldnt be able to move any more
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testUnloadingTransportsCantMove() {
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 4);
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(equatorialAfrica);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    map = new IntegerMap<>();
    map.put(transport, 2);
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);
    // the transports unloaded so they cant move
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testTransportsCanSplit() {
    // move 1 armour to red sea
    Route route = new Route();
    route.setStart(egypt);
    route.add(redSea);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);
    // move two infantry to red sea
    route = new Route();
    route.setStart(eastAfrica);
    route.add(redSea);
    map = new IntegerMap<>();
    map.put(infantry, 2);
    results = delegate.move(getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);
    // try to move 1 transport to indian ocean with 1 tank
    route = new Route();
    route.setStart(redSea);
    route.add(indianOcean);
    map = new IntegerMap<>();
    map.put(armour, 1);
    map.put(transport, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // move the other transport to west compass
    route = new Route();
    route.setStart(redSea);
    route.add(westCompass);
    map = new IntegerMap<>();
    map.put(infantry, 2);
    map.put(transport, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testUseTransportsWithLowestMovement() {
    // move transport south
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(angolaSeaZone);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(transport, 1);
    map.put(infantry, 2);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // move transport back
    route = new Route();
    route.setStart(angolaSeaZone);
    route.add(congoSeaZone);
    map = new IntegerMap<>();
    map.put(transport, 1);
    map.put(infantry, 2);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // move the other transport south, should
    // figure out that only 1 can move
    // and will choose that one
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(angolaSeaZone);
    map = new IntegerMap<>();
    map.put(infantry, 2);
    map.put(transport, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testCanOverrunNeutralWithoutFunds() {
    assertEquals(35, british.getResources().getQuantity(pus));
    final Change makePoor = ChangeFactory.changeResourcesChange(british, pus, -35);
    bridge.addChange(makePoor);
    assertEquals(0, british.getResources().getQuantity(pus));
    // try to take over South Africa, cant because we cant afford it
    final Route route = new Route();
    route.setStart(egypt);
    route.add(kenya);
    route.add(southAfrica);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testAirViolateNeutrality() {
    final Route route = new Route();
    route.setStart(egypt);
    route.add(kenya);
    route.add(southAfrica);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 2);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testNeutralConquered() {
    // take over neutral
    final Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 1);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    assertTrue(DelegateFinder.battleDelegate(gameData).getBattleTracker().wasConquered(westAfrica));
    assertTrue(!DelegateFinder.battleDelegate(gameData).getBattleTracker().wasBlitzed(westAfrica));
  }

  @Test
  public void testMoveTransportsTwice() {
    // move transports
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 2);
    map.put(transport, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // move again
    route = new Route();
    route.setStart(southAtlantic);
    route.add(angolaSeaZone);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testCantMoveThroughConqueredNeutral() {
    // take over neutral
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // make sure we cant move through it by land
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    route.add(algeria);
    map = new IntegerMap<>();
    map.put(armour, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
    // make sure we can still move units to the territory
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    map = new IntegerMap<>();
    map.put(armour, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // make sure air can though
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);
    route.add(westAfrica);
    route.add(equatorialAfrica);
    map = new IntegerMap<>();
    map.put(fighter, 3);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testCanBlitzThroughConqueredEnemy() {
    // take over empty enemy
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // make sure we can still blitz through it
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);
    route.add(algeria);
    map = new IntegerMap<>();
    map.put(armour, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testAirCantLandInConquered() {
    // take over empty neutral
    Route route = new Route();
    route.setStart(egypt);
    route.add(kenya);
    route.add(southAfrica);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // move carriers to ensure they can't go anywhere
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSea);
    route.add(northAtlantic);
    Collection<Unit> units = new ArrayList<>();
    units.addAll(Match.getMatches(gameData.getMap().getTerritory(congoSeaZone.toString()).getUnits().getUnits(),
        Matches.UnitIsCarrier));
    results = delegate.move(units, route);
    assertValid(results);
    // move carriers to ensure they can't go anywhere
    route = new Route();
    route.setStart(redSea);
    route.add(eastMediteranean);
    route.add(blackSea);
    units = new ArrayList<>();
    units.addAll(Match.getMatches(
        gameData.getMap().getTerritory(redSea.toString()).getUnits().getUnits(), Matches.UnitIsCarrier));
    results = delegate.move(units, route);
    assertValid(results);
    // make sure the place cant use it to land
    // the only possibility would be newly conquered south africa
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(angolaSeaZone);
    route.add(southAfricaSeaZone);
    map = new IntegerMap<>();
    map.put(fighter, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testMoveAndTransportUnload() {
    // this was causing an exception
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(westAfricaSeaZone);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(transport, 1);
    map.put(infantry, 2);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    route = new Route();
    route.setStart(westAfricaSeaZone);
    route.add(westAfrica);
    map = new IntegerMap<>();
    map.put(infantry, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testTakeOverAfterOverFlight() {
    // this was causing an exception
    Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(bomber, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    route = new Route();
    route.setStart(libya);
    route.add(algeria);
    // planes cannot leave a battle zone, but the territory was empty so no battle occurred
    map = new IntegerMap<>();
    map.put(bomber, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testBattleAdded() {
    // TODO if air make sure otnot alwasys battle
    // this was causing an exception
    final Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(bomber, 1);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testLargeMove() {
    // was causing an error
    final Route route = new Route();
    route.setStart(egypt);
    route.add(libya);
    route.add(algeria);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(bomber, 6);
    map.put(fighter, 6);
    map.put(armour, 6);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testAmphibiousAssaultAfterNavalBattle() {
    // move to take on brazil navy
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southBrazilSeaZone);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(transport, 2);
    map.put(infantry, 4);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // try to unload transports
    route = new Route();
    route.setStart(southBrazilSeaZone);
    route.add(brazil);
    map = new IntegerMap<>();
    map.put(infantry, 4);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    final IBattle inBrazil =
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getPendingBattle(brazil, false, null);
    final IBattle inBrazilSea =
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getPendingBattle(southBrazilSeaZone, false, null);
    assertNotNull(inBrazilSea);
    assertNotNull(inBrazil);
    assertEquals(DelegateFinder.battleDelegate(gameData).getBattleTracker().getDependentOn(inBrazil).iterator().next(),
        inBrazilSea);
  }

  @Test
  public void testReloadTransportAfterRetreatAmphibious() {
    bridge = super.getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    Route route = new Route();
    route.setStart(northSea);
    route.add(balticSeaZone);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(transport, 1);
    map.put(infantry, 2);
    // Move from the NorthSea to the BalticSea and validate the move
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Unload transports into Finland and validate
    route = new Route();
    route.setStart(balticSeaZone);
    route.add(finlandNorway);
    map = new IntegerMap<>();
    map.put(infantry, 2);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Get the attacking sea units that will retreat
    final List<Unit> retreatingSeaUnits = new ArrayList<>();
    retreatingSeaUnits.addAll(balticSeaZone.getUnits().getMatches(Matches.enemyUnit(germans, gameData)));
    // Get the attacking land units that will retreat and their number
    final List<Unit> retreatingLandUnits = new ArrayList<>();
    retreatingLandUnits.addAll(finlandNorway.getUnits().getMatches(Matches.enemyUnit(germans, gameData)));
    final int retreatingLandSizeInt = retreatingLandUnits.size();
    // Get the defending land units that and their number
    final List<Unit> defendingLandUnits = new ArrayList<>();
    defendingLandUnits.addAll(finlandNorway.getUnits().getMatches(Matches.enemyUnit(british, gameData)));
    final int defendingLandSizeInt = defendingLandUnits.size();
    // Set up the battles and the dependent battles
    final IBattle inFinlandNorway =
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getPendingBattle(finlandNorway, false, null);
    final IBattle inBalticSeaZone =
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getPendingBattle(balticSeaZone, false, null);
    assertNotNull(balticSeaZone);
    assertNotNull(finlandNorway);
    assertEquals(
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getDependentOn(inFinlandNorway).iterator().next(),
        inBalticSeaZone);
    // Add some defending units in case there aren't any
    final List<Unit> defendList = transport.create(1, germans);
    final List<Unit> defendSub = submarine.create(1, germans);
    defendList.addAll(defendSub);
    // fire the defending transport then the submarine (both miss)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1, 2}));
    // Execute the battle and verify no hits
    final DiceRoll roll = DiceRoll.rollDice(defendList, true, germans, bridge, new MockBattle(balticSeaZone), "",
        TerritoryEffectHelper.getEffects(balticSeaZone), null);
    assertEquals(0, roll.getHits());
    // Get total number of units in Finland before the retreat
    final int preCountInt = finlandNorway.getUnits().size();
    // Retreat from the Baltic
    ((MustFightBattle) inBalticSeaZone).externalRetreat(retreatingSeaUnits, northSea, false, bridge);
    // Get the total number of units that should be left
    final int postCountInt = preCountInt - retreatingLandSizeInt;
    // Compare the number of units in Finland to begin with the number after retreating
    assertEquals(defendingLandSizeInt, postCountInt);
  }

  @Test
  public void testReloadTransportAfterDyingAmphibious() {
    bridge = super.getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    Route route = new Route();
    route.setStart(northSea);
    route.add(balticSeaZone);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(transport, 1);
    map.put(infantry, 2);
    // Move from the NorthSea to the BalticSea and validate the move
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Unload transports into Finland and validate
    route = new Route();
    route.setStart(balticSeaZone);
    route.add(finlandNorway);
    map = new IntegerMap<>();
    map.put(infantry, 2);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Get the attacking sea units that will retreat
    final List<Unit> retreatingSeaUnits = new ArrayList<>();
    retreatingSeaUnits.addAll(balticSeaZone.getUnits().getMatches(Matches.enemyUnit(germans, gameData)));
    // Get the attacking land units that will retreat and their number
    final List<Unit> retreatingLandUnits = new ArrayList<>();
    retreatingLandUnits.addAll(finlandNorway.getUnits().getMatches(Matches.enemyUnit(germans, gameData)));
    final int retreatingLandSizeInt = retreatingLandUnits.size();
    // Get the defending land units that and their number
    final List<Unit> defendingLandUnits = new ArrayList<>();
    defendingLandUnits.addAll(finlandNorway.getUnits().getMatches(Matches.enemyUnit(british, gameData)));
    final int defendingLandSizeInt = defendingLandUnits.size();
    // Set up the battles and the dependent battles
    final IBattle inFinlandNorway =
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getPendingBattle(finlandNorway, false, null);
    final IBattle inBalticSeaZone =
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getPendingBattle(balticSeaZone, false, null);
    assertNotNull(balticSeaZone);
    assertNotNull(finlandNorway);
    assertEquals(
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getDependentOn(inFinlandNorway).iterator().next(),
        inBalticSeaZone);
    // Add some defending units in case there aren't any
    final List<Unit> defendList = transport.create(1, germans);
    final List<Unit> defendSub = submarine.create(1, germans);
    defendList.addAll(defendSub);
    // fire the defending transport then the submarine (One hit)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 2}));
    // Execute the battle and verify no hits
    final DiceRoll roll = DiceRoll.rollDice(defendList, true, germans, bridge, new MockBattle(balticSeaZone), "",
        TerritoryEffectHelper.getEffects(balticSeaZone), null);
    assertEquals(1, roll.getHits());
    // Get total number of units in Finland before the retreat
    final int preCountInt = finlandNorway.getUnits().size();
    // Retreat from the Baltic
    ((MustFightBattle) inBalticSeaZone).externalRetreat(retreatingSeaUnits, northSea, false, bridge);
    // Get the total number of units that should be left
    final int postCountInt = preCountInt - retreatingLandSizeInt;
    // Compare the number of units in Finland to begin with the number after retreating
    assertEquals(defendingLandSizeInt, postCountInt);
  }

  @Test
  public void testReloadTransportAfterRetreatAllied() {
    bridge = super.getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    Route route = new Route();
    route.setStart(northSea);
    route.add(balticSeaZone);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(transport, 1);
    map.put(infantry, 2);
    // Move from the NorthSea to the BalticSea and validate the move
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Unload transports into Finland and validate
    route = new Route();
    route.setStart(balticSeaZone);
    route.add(karelia);
    map = new IntegerMap<>();
    map.put(infantry, 2);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Get the attacking sea units that will retreat
    final List<Unit> retreatingSeaUnits = new ArrayList<>();
    retreatingSeaUnits.addAll(balticSeaZone.getUnits().getMatches(Matches.enemyUnit(germans, gameData)));
    // Get the attacking land units that will retreat and their number
    final List<Unit> retreatingLandUnits = new ArrayList<>();
    retreatingLandUnits.addAll(karelia.getUnits().getMatches(Matches.isUnitAllied(russians, gameData)));
    final int retreatingLandSizeInt = retreatingLandUnits.size();
    // Get the defending land units that and their number
    retreatingLandUnits.addAll(karelia.getUnits().getMatches(Matches.isUnitAllied(british, gameData)));
    final List<Unit> defendingLandUnits = new ArrayList<>();
    final int defendingLandSizeInt = defendingLandUnits.size();
    // Set up the battles and the dependent battles
    final IBattle inBalticSeaZone =
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getPendingBattle(balticSeaZone, false, null);
    assertNotNull(balticSeaZone);
    // Add some defending units in case there aren't any
    final List<Unit> defendList = transport.create(1, germans);
    final List<Unit> defendSub = submarine.create(1, germans);
    defendList.addAll(defendSub);
    // fire the defending transport then the submarine (both miss)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1, 2}));
    // Execute the battle and verify no hits
    final DiceRoll roll = DiceRoll.rollDice(defendList, true, germans, bridge, new MockBattle(balticSeaZone), "",
        TerritoryEffectHelper.getEffects(balticSeaZone), null);
    assertEquals(0, roll.getHits());
    // Get total number of units in Finland before the retreat
    final int preCountInt = karelia.getUnits().size();
    // Retreat from the Baltic
    ((MustFightBattle) inBalticSeaZone).externalRetreat(retreatingSeaUnits, northSea, false, bridge);
    // Get the total number of units that should be left
    final int postCountInt = preCountInt - retreatingLandSizeInt;
    // Compare the number of units in Finland to begin with the number after retreating
    assertEquals(defendingLandSizeInt, postCountInt);
  }

  @Test
  public void testReloadTransportAfterDyingAllied() {
    bridge = super.getDelegateBridge(british);
    bridge.setStepName("britishCombatMove");
    Route route = new Route();
    route.setStart(northSea);
    route.add(balticSeaZone);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(transport, 1);
    map.put(infantry, 2);
    // Move from the NorthSea to the BalticSea and validate the move
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Unload transports into Finland and validate
    route = new Route();
    route.setStart(balticSeaZone);
    route.add(karelia);
    map = new IntegerMap<>();
    map.put(infantry, 2);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // Get the attacking sea units that will retreat
    final List<Unit> retreatingSeaUnits = new ArrayList<>();
    retreatingSeaUnits.addAll(balticSeaZone.getUnits().getMatches(Matches.enemyUnit(germans, gameData)));
    // Get the attacking land units that will retreat and their number
    final List<Unit> retreatingLandUnits = new ArrayList<>();
    retreatingLandUnits.addAll(karelia.getUnits().getMatches(Matches.isUnitAllied(russians, gameData)));
    final int retreatingLandSizeInt = retreatingLandUnits.size();
    // Get the defending land units that and their number
    final List<Unit> defendingLandUnits = new ArrayList<>();
    retreatingLandUnits.addAll(karelia.getUnits().getMatches(Matches.isUnitAllied(british, gameData)));
    final int defendingLandSizeInt = defendingLandUnits.size();
    // Set up the battles and the dependent battles
    final IBattle inBalticSeaZone =
        DelegateFinder.battleDelegate(gameData).getBattleTracker().getPendingBattle(balticSeaZone, false, null);
    assertNotNull(balticSeaZone);
    // Add some defending units in case there aren't any
    final List<Unit> defendList = transport.create(1, germans);
    final List<Unit> defendSub = submarine.create(1, germans);
    defendList.addAll(defendSub);
    // fire the defending transport then the submarine (One hit)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 2}));
    // Execute the battle and verify no hits
    final DiceRoll roll = DiceRoll.rollDice(defendList, true, germans, bridge, new MockBattle(balticSeaZone), "",
        TerritoryEffectHelper.getEffects(balticSeaZone), null);
    assertEquals(1, roll.getHits());
    // Get total number of units in Finland before the retreat
    final int preCountInt = karelia.getUnits().size();
    // Retreat from the Baltic
    ((MustFightBattle) inBalticSeaZone).externalRetreat(retreatingSeaUnits, northSea, false, bridge);
    // Get the total number of units that should be left
    final int postCountInt = preCountInt - retreatingLandSizeInt;
    // Compare the number of units in Finland to begin with the number after retreating
    assertEquals(defendingLandSizeInt, postCountInt);
  }

  @Test
  public void testAirToWater() {
    final Route route = new Route();
    route.setStart(egypt);
    route.add(eastMediteranean);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 3);
    map.put(bomber, 3);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testNonCombatAttack() {
    bridge.setStepName("britishNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(algeria);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testNonCombatAttackNeutral() {
    bridge.setStepName("britishNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testNonCombatMoveToConquered() {
    // take over libya
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // go to non combat
    bridge.setStepName("britishNonCombatMove");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    // move more into libya
    route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);
    map = new IntegerMap<>();
    map.put(armour, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testAaCantMoveToConquered() {
    bridge.setStepName("japaneseCombatMove");
    bridge.setPlayerId(japanese);
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
    final Route route = new Route();
    route.setStart(congo);
    route.add(kenya);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(armour, 2);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    final BattleTracker tracker = DelegateFinder.battleDelegate(gameData).getBattleTracker();
    assertTrue(tracker.wasBlitzed(kenya));
    assertTrue(tracker.wasConquered(kenya));
    map.clear();
    map.put(aaGun, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testBlitzConqueredNeutralInTwoSteps() {
    Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(westAfrica);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    final BattleTracker tracker = DelegateFinder.battleDelegate(gameData).getBattleTracker();
    assertTrue(!tracker.wasBlitzed(westAfrica));
    assertTrue(tracker.wasConquered(westAfrica));
    map.clear();
    map.put(armour, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    route = new Route();
    route.setStart(westAfrica);
    route.add(algeria);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertError(results);
  }

  @Test
  public void testBlitzFactory() {
    // create a factory to be taken
    final Collection<Unit> factCollection = factory.create(1, japanese);
    final Change addFactory = ChangeFactory.addUnits(libya, factCollection);
    bridge.addChange(addFactory);
    final Route route = new Route();
    route.setStart(equatorialAfrica);
    route.add(libya);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 1);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    final BattleTracker tracker = DelegateFinder.battleDelegate(gameData).getBattleTracker();
    assertTrue(tracker.wasBlitzed(libya));
    assertTrue(tracker.wasConquered(libya));
    final Unit aFactory = factCollection.iterator().next();
    assertEquals(aFactory.getOwner(), british);
  }

  @Test
  public void testAirCanLandOnLand() {
    final Route route = new Route();
    route.setStart(egypt);
    route.add(eastMediteranean);
    route.add(blackSea);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testAirDifferingRouts() {
    // move one air unit 3 spaces, and a second 2,
    // this was causing an exception when the validator tried to find if they
    // could both land
    // EW: I don't know why this test is failing or what it is supposed to do...
    Route route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(antarticSea);
    route.add(angolaSeaZone);
    IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    route = new Route();
    route.setStart(congoSeaZone);
    route.add(southAtlantic);
    route.add(antarticSea);
    route.add(angolaSeaZone);
    map = new IntegerMap<>();
    map.put(fighter, 1);
    results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testRoute() {
    final Route route = gameData.getMap().getRoute(angola, russia);
    assertNotNull(route);
    assertEquals(route.getEnd(), russia);
  }
}
