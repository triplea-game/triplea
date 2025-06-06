package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.assertMoveError;
import static games.strategy.triplea.delegate.GameDataTestUtil.load;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.removeFrom;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class PacificTest {
  private final GameData gameData = TestMapGameData.PACIFIC_INCOMPLETE.getGameData();
  private IDelegateBridge bridge;
  private MoveDelegate delegate;

  private final UnitType infantry = GameDataTestUtil.infantry(gameData);
  private final UnitType marine = GameDataTestUtil.marine(gameData);
  private final UnitType transport = GameDataTestUtil.transport(gameData);
  private final UnitType submarine = GameDataTestUtil.submarine(gameData);
  private final UnitType destroyer = GameDataTestUtil.destroyer(gameData);
  private final UnitType carrier = GameDataTestUtil.carrier(gameData);
  private final UnitType fighter = GameDataTestUtil.fighter(gameData);
  // Define players
  private final GamePlayer americans = GameDataTestUtil.americans(gameData);
  private final GamePlayer chinese = GameDataTestUtil.chinese(gameData);
  private final GamePlayer japanese = GameDataTestUtil.japanese(gameData);
  // Define territories
  private final Territory queensland = territory("Queensland", gameData);
  private final Territory unitedStates = territory("United States", gameData);
  private final Territory newBritain = territory("New Britain", gameData);
  private final Territory midway = territory("Midway", gameData);
  private final Territory bonin = territory("Bonin", gameData);
  private final Territory canada = territory("Canada", gameData);
  // Define Sea Zones
  private final Territory sz4 = territory("4 Sea Zone", gameData);
  private final Territory sz5 = territory("5 Sea Zone", gameData);
  private final Territory sz7 = territory("7 Sea Zone", gameData);
  private final Territory sz8 = territory("8 Sea Zone", gameData);
  private final Territory sz10 = territory("10 Sea Zone", gameData);
  private final Territory sz14 = territory("14 Sea Zone", gameData);
  private final Territory sz16 = territory("16 Sea Zone", gameData);
  private final Territory sz20 = territory("20 Sea Zone", gameData);
  private final Territory sz21 = territory("21 Sea Zone", gameData);
  private final Territory sz24 = territory("24 Sea Zone", gameData);
  private final Territory sz27 = territory("27 Sea Zone", gameData);
  private final Territory sz30 = territory("30 Sea Zone", gameData);

  @BeforeEach
  void setupPacificTest() {
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
        RollDiceFactory.rollBattleDice(
            infantryUs,
            americans,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(infantryUs)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(1, roll.getHits());
    // Defending US marines
    final List<Unit> marineUs = marine.create(1, americans);
    roll =
        RollDiceFactory.rollBattleDice(
            marineUs,
            americans,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(marineUs)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(1, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    final List<Unit> infantryChina = infantry.create(1, chinese);
    roll =
        RollDiceFactory.rollBattleDice(
            infantryChina,
            chinese,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(infantryChina)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
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
        RollDiceFactory.rollBattleDice(
            infantryUs,
            americans,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(infantryUs)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(0, roll.getHits());
    // Defending US marines
    final List<Unit> marineUs = marine.create(1, americans);
    roll =
        RollDiceFactory.rollBattleDice(
            marineUs,
            americans,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(marineUs)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(0, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    final List<Unit> infantryChina = infantry.create(1, chinese);
    roll =
        RollDiceFactory.rollBattleDice(
            infantryChina,
            chinese,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(infantryChina)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(1, roll.getHits());
    // Defending US infantry
    roll =
        RollDiceFactory.rollBattleDice(
            infantryUs,
            americans,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(infantryUs)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(1, roll.getHits());
    // Defending US marines
    roll =
        RollDiceFactory.rollBattleDice(
            marineUs,
            americans,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(marineUs)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
    assertEquals(1, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    roll =
        RollDiceFactory.rollBattleDice(
            infantryChina,
            chinese,
            bridge,
            "",
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(List.of())
                .friendlyUnits(infantryChina)
                .side(BattleState.Side.DEFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build());
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
  void testCanMoveFurtherBetweenNavalBases() throws MutableProperty.InvalidValueException {
    // Remove Japanese units from sz20 so that we can move ships there non-combat.
    removeFrom(sz20, sz20.getUnits());

    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(submarine, 1);
    addTo(sz5, submarine.create(10, americans));
    map.put(transport, 1);
    addTo(sz5, transport.create(10, americans));
    map.put(destroyer, 1);
    addTo(sz5, destroyer.create(10, americans));
    map.put(carrier, 1);
    addTo(sz5, carrier.create(10, americans));

    advanceToStep(bridge, "americanCombatMove");
    // During combat move, naval bases do not boost movement.
    final Route toSz20 = new Route(sz5, sz7, sz8, sz20);
    assertMoveError(GameDataTestUtil.getUnits(map, toSz20.getStart()), toSz20);

    // But they do, during non-combat.
    advanceToStep(bridge, "americanNonCombatMove");
    // Moving 3 spaces to sz20 is allowed, since there's a Naval base at the destination.
    move(GameDataTestUtil.getUnits(map, toSz20.getStart()), toSz20);

    // Moving 3 spaces to sz21 is not allowed, since there is no naval base at destination.
    final Route toSz21 = new Route(sz5, sz7, sz8, sz21);
    assertMoveError(GameDataTestUtil.getUnits(map, toSz21.getStart()), toSz21);

    // Going 3 spaces to sz14 is OK, since it's an allied naval base.
    final Route toSz14 = new Route(sz5, sz4, sz10, sz14);
    move(GameDataTestUtil.getUnits(map, toSz14.getStart()), toSz14);

    // But can't go 4 spaces to sz30, even with a base at the destination.
    final Route toSz30 = new Route(sz5, sz4, sz10, sz14, sz30);
    assertMoveError(GameDataTestUtil.getUnits(map, toSz30.getStart()), toSz30);

    // Finally, adding a naval base in Canada shouldn't boost movement further.
    TerritoryAttachment.getOrThrow(canada).getPropertyOrThrow("navalBase").setValue(true);
    assertThat(TerritoryAttachment.hasNavalBase(canada), is(true));
    // Should still fail to move 4.
    assertMoveError(GameDataTestUtil.getUnits(map, toSz30.getStart()), toSz30);
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
