package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.load;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;
import org.triplea.test.common.Integration;
import org.triplea.test.common.TestType;

@Integration(type = TestType.ACCEPTANCE)
class PacificTest extends AbstractDelegateTestCase {
  private UnitType marine;
  // Define players
  private PlayerId americans;
  private PlayerId chinese;
  // Define territories
  private Territory queensland;
  private Territory unitedStates;
  private Territory newBritain;
  private Territory midway;
  private Territory bonin;
  // Define Sea Zones
  private Territory sz4;
  private Territory sz5;
  private Territory sz7;
  private Territory sz8;
  private Territory sz10;
  private Territory sz16;
  private Territory sz20;
  private Territory sz24;
  private Territory sz27;
  private IDelegateBridge bridge;
  private MoveDelegate delegate;

  @BeforeEach
  void setupPacificTest() {
    gameData = TestMapGameData.PACIFIC_INCOMPLETE.getGameData();
    // Define units
    infantry = GameDataTestUtil.infantry(gameData);
    marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    fighter = GameDataTestUtil.fighter(gameData);
    bomber = GameDataTestUtil.bomber(gameData);
    carrier = GameDataTestUtil.carrier(gameData);
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
    sz27 = gameData.getMap().getTerritory("27 Sea Zone");
    bridge = newDelegateBridge(americans);
    advanceToStep(bridge, "japaneseCombatMove");
    delegate = new MoveDelegate();
    delegate.initialize("move", "MoveDelegate");
    delegate.setDelegateBridgeAndPlayer(bridge);
    gameData.addDelegate(delegate);
    delegate.start();
  }

  @Test
  void testNonJapanAttack() {
    // this will get us to round 2
    advanceToStep(bridge, "japaneseEndTurn");
    advanceToStep(bridge, "japaneseBattle");
    final List<Unit> infantryUs = infantry.create(1, americans);
    final Collection<TerritoryEffect> territoryEffects =
        TerritoryEffectHelper.getEffects(queensland);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // Defending US infantry hit on a 2 (0 base)
        .thenAnswer(withValues(1)) // Defending US marines hit on a 2 (0 base)
        .thenAnswer(withValues(1)); // Defending Chinese infantry hit on a 2 (0 base)
    // Defending US infantry
    DiceRoll roll =
        DiceRoll.rollDice(
            infantryUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US marines
    final List<Unit> marineUs = marine.create(1, americans);
    roll =
        DiceRoll.rollDice(
            marineUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    final List<Unit> infantryChina = infantry.create(1, chinese);
    roll =
        DiceRoll.rollDice(
            infantryChina, true, chinese, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
  }

  @Test
  void testJapanAttackFirstRound() {
    advanceToStep(bridge, "japaneseBattle");
    while (!gameData.getSequence().getStep().getName().equals("japaneseBattle")) {
      gameData.getSequence().next();
    }
    // >>> After patch normal to-hits will miss <<<
    final List<Unit> infantryUs = infantry.create(1, americans);
    final Collection<TerritoryEffect> territoryEffects =
        TerritoryEffectHelper.getEffects(queensland);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // Defending US infantry miss on a 2 (0 base)
        .thenAnswer(withValues(1)) // Defending US marines miss on a 2 (0 base)
        .thenAnswer(withValues(1)) // Defending Chinese infantry still hit on a 2 (0 base)
        .thenAnswer(withValues(0)) // Defending US infantry hit on a 1 (0 base)
        .thenAnswer(withValues(0)) // Defending US marines hit on a 1 (0 base)
        .thenAnswer(withValues(1)); // Defending Chinese infantry still hit on a 2 (0 base)
    // Defending US infantry
    DiceRoll roll =
        DiceRoll.rollDice(
            infantryUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(0, roll.getHits());
    // Defending US marines
    final List<Unit> marineUs = marine.create(1, americans);
    roll =
        DiceRoll.rollDice(
            marineUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(0, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    final List<Unit> infantryChina = infantry.create(1, chinese);
    roll =
        DiceRoll.rollDice(
            infantryChina, true, chinese, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US infantry
    roll =
        DiceRoll.rollDice(
            infantryUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US marines
    roll =
        DiceRoll.rollDice(
            marineUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    roll =
        DiceRoll.rollDice(
            infantryChina, true, chinese, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
  }

  @Test
  void testCanLand2Airfields() {
    advanceToStep(bridge, "americanCombatMove");
    final Route route = new Route(unitedStates, sz5, sz4, sz10, sz16, sz27, newBritain);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    move(GameDataTestUtil.getUnits(map, route.getStart()), route);
  }

  @Test
  void testCanLand1AirfieldStart() {
    advanceToStep(bridge, "americanCombatMove");
    final Route route = new Route(unitedStates, sz5, sz7, sz8, sz20, midway);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    move(GameDataTestUtil.getUnits(map, route.getStart()), route);
  }

  @Test
  void testCanLand1AirfieldEnd() {
    advanceToStep(bridge, "americanCombatMove");
    final Route route = new Route(unitedStates, sz5, sz7, sz8, sz20, midway);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    move(GameDataTestUtil.getUnits(map, route.getStart()), route);
  }

  @Test
  void testCanMoveNavalBase() {
    advanceToStep(bridge, "americanNonCombatMove");
    final Route route = new Route(sz5, sz7, sz8, sz20);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    move(GameDataTestUtil.getUnits(map, route.getStart()), route);
  }

  @Test
  void testJapaneseDestroyerTransport() {
    bridge = newDelegateBridge(japanese);
    delegate = new MoveDelegate();
    delegate.initialize("move", "MoveDelegate");
    delegate.setDelegateBridgeAndPlayer(bridge);
    gameData.addDelegate(delegate);
    advanceToStep(bridge, "japaneseNonCombatMove");
    delegate.start();

    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 1);
    final Route route = new Route(bonin, sz24);
    // movement to force boarding
    // verify unit counts before move
    assertEquals(2, bonin.getUnitCollection().size());
    assertEquals(1, sz24.getUnitCollection().size());
    // validate movement
    load(GameDataTestUtil.getUnits(map, route.getStart()), route);
    // verify unit counts after move
    assertEquals(1, bonin.getUnitCollection().size());
    assertEquals(2, sz24.getUnitCollection().size());
  }
}
