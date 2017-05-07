package games.strategy.triplea.delegate;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.IntegerMap;

// public class PacificTest extends TestCase
public class PacificTest extends DelegateTest {
  UnitType armor;
  UnitType artillery;
  UnitType marine;
  UnitType sub;
  UnitType destroyer;
  UnitType battleship;
  // Define players
  PlayerID americans;
  PlayerID chinese;
  // Define territories
  Territory queensland;
  Territory unitedStates;
  Territory newBritain;
  Territory midway;
  Territory mariana;
  Territory bonin;
  // Define Sea Zones
  Territory sz4;
  Territory sz5;
  Territory sz7;
  Territory sz8;
  Territory sz10;
  Territory sz16;
  Territory sz20;
  Territory sz24;
  Territory sz25;
  Territory sz27;
  ITestDelegateBridge bridge;
  MoveDelegate delegate;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    gameData = TestMapGameData.PACIFIC_INCOMPLETE.getGameData();
    // Define units
    infantry = GameDataTestUtil.infantry(gameData);
    armor = GameDataTestUtil.armour(gameData);
    artillery = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_ARTILLERY);
    marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    fighter = GameDataTestUtil.fighter(gameData);
    bomber = GameDataTestUtil.bomber(gameData);
    sub = GameDataTestUtil.submarine(gameData);
    destroyer = GameDataTestUtil.destroyer(gameData);
    carrier = GameDataTestUtil.carrier(gameData);
    battleship = GameDataTestUtil.battleship(gameData);
    transport = GameDataTestUtil.transport(gameData);
    // Define players
    americans = GameDataTestUtil.americans(gameData);
    chinese = GameDataTestUtil.chinese(gameData);
    british = GameDataTestUtil.british(gameData);
    japanese = GameDataTestUtil.japanese(gameData);
    // Define territories
    queensland = gameData.getMap().getTerritory("Queensland");
    japan = gameData.getMap().getTerritory("Japan");
    unitedStates = gameData.getMap().getTerritory("United States");
    newBritain = gameData.getMap().getTerritory("New Britain");
    midway = gameData.getMap().getTerritory("Midway");
    mariana = gameData.getMap().getTerritory("Mariana");
    bonin = gameData.getMap().getTerritory("Bonin");
    // Define Sea Zones
    sz4 = gameData.getMap().getTerritory("4 Sea Zone");
    sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    sz7 = gameData.getMap().getTerritory("7 Sea Zone");
    sz8 = gameData.getMap().getTerritory("8 Sea Zone");
    sz10 = gameData.getMap().getTerritory("10 Sea Zone");
    sz16 = gameData.getMap().getTerritory("16 Sea Zone");
    sz20 = gameData.getMap().getTerritory("20 Sea Zone");
    sz24 = gameData.getMap().getTerritory("24 Sea Zone");
    sz25 = gameData.getMap().getTerritory("25 Sea Zone");
    sz27 = gameData.getMap().getTerritory("27 Sea Zone");
    bridge = getDelegateBridge(americans);
    bridge.setStepName("japaneseCombatMove");
    delegate = new MoveDelegate();
    delegate.initialize("MoveDelegate", "MoveDelegate");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  private Collection<Unit> getUnits(final IntegerMap<UnitType> units, final Territory from) {
    final Iterator<UnitType> iter = units.keySet().iterator();
    final Collection<Unit> rVal = new ArrayList<>(units.totalValues());
    while (iter.hasNext()) {
      final UnitType type = iter.next();
      rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
    }
    return rVal;
  }

  @Override
  protected ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, gameData);
  }

  @Test
  public void testNonJapanAttack() {
    // this will get us to round 2
    bridge.setStepName("japaneseEndTurn");
    bridge.setStepName("japaneseBattle");
    // Defending US infantry hit on a 2 (0 base)
    final List<Unit> infantryUS = infantry.create(1, americans);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(queensland);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    DiceRoll roll =
        DiceRoll.rollDice(infantryUS, true, americans, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US marines hit on a 2 (0 base)
    final List<Unit> marineUS = marine.create(1, americans);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    roll = DiceRoll.rollDice(marineUS, true, americans, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Chinese units
    // Defending Chinese infantry hit on a 2 (0 base)
    final List<Unit> infantryChina = infantry.create(1, chinese);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    roll =
        DiceRoll.rollDice(infantryChina, true, chinese, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
  }

  @Test
  public void testJapanAttackFirstRound() {
    bridge.setStepName("japaneseBattle");
    while (!gameData.getSequence().getStep().getName().equals("japaneseBattle")) {
      gameData.getSequence().next();
    }
    // >>> After patch normal to-hits will miss <<<
    // Defending US infantry miss on a 2 (0 base)
    final List<Unit> infantryUS = infantry.create(1, americans);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(queensland);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    DiceRoll roll =
        DiceRoll.rollDice(infantryUS, true, americans, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(0, roll.getHits());
    // Defending US marines miss on a 2 (0 base)
    final List<Unit> marineUS = marine.create(1, americans);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    roll = DiceRoll.rollDice(marineUS, true, americans, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(0, roll.getHits());
    // Chinese units
    // Defending Chinese infantry still hit on a 2 (0 base)
    final List<Unit> infantryChina = infantry.create(1, chinese);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    roll =
        DiceRoll.rollDice(infantryChina, true, chinese, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US infantry hit on a 1 (0 base)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0}));
    roll =
        DiceRoll.rollDice(infantryUS, true, americans, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US marines hit on a 1 (0 base)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0}));
    roll = DiceRoll.rollDice(marineUS, true, americans, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Chinese units
    // Defending Chinese infantry still hit on a 2 (0 base)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    roll =
        DiceRoll.rollDice(infantryChina, true, chinese, bridge, new MockBattle(queensland), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
  }

  @Test
  public void testCanLand2Airfields() {
    bridge.setStepName("americanCombatMove");
    final Route route = new Route();
    route.setStart(unitedStates);
    route.add(sz5);
    route.add(sz4);
    route.add(sz10);
    route.add(sz16);
    route.add(sz27);
    route.add(newBritain);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testCanLand1AirfieldStart() {
    bridge.setStepName("americanCombatMove");
    final Route route = new Route();
    route.setStart(unitedStates);
    route.add(sz5);
    route.add(sz7);
    route.add(sz8);
    route.add(sz20);
    route.add(midway);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
    // assertError( results);
  }

  @Test
  public void testCanLand1AirfieldEnd() {
    bridge.setStepName("americanCombatMove");
    final Route route = new Route();
    route.setStart(unitedStates);
    route.add(sz5);
    route.add(sz7);
    route.add(sz8);
    route.add(sz20);
    route.add(midway);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testCanMoveNavalBase() {
    bridge.setStepName("americanNonCombatMove");
    final Route route = new Route();
    route.setStart(sz5);
    route.add(sz7);
    route.add(sz8);
    route.add(sz20);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testJapaneseDestroyerTransport() {
    bridge = getDelegateBridge(japanese);
    delegate = new MoveDelegate();
    delegate.initialize("MoveDelegate", "MoveDelegate");
    delegate.setDelegateBridgeAndPlayer(bridge);
    bridge.setStepName("japaneseNonCombatMove");
    delegate.start();
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 1);
    final Route route = new Route();
    route.setStart(bonin);
    // movement to force boarding
    route.add(sz24);
    // verify unit counts before move
    assertEquals(2, bonin.getUnits().size());
    assertEquals(1, sz24.getUnits().size());
    // validate movement
    final String results =
        delegate.move(getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);
    // verify unit counts after move
    assertEquals(1, bonin.getUnits().size());
    assertEquals(2, sz24.getUnits().size());
  }
}
