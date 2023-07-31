package games.strategy.triplea.delegate.battle.casualty;

import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.makeGameLowLuck;
import static games.strategy.triplea.delegate.GameDataTestUtil.setSelectAaCasualties;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.xml.TestMapGameData;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

class CasualtySelectorTest {
  private IDelegateBridge bridge;

  private void givenRemotePlayerWillSelectStrategicBombersForCasualties() {
    when(bridge
            .getRemotePlayer()
            .selectCasualties(
                any(),
                any(),
                anyInt(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any(),
                any(),
                any(),
                any(),
                anyBoolean()))
        .thenAnswer(
            invocation -> {
              final Collection<Unit> selectFrom = invocation.getArgument(0);
              final int count = invocation.getArgument(2);
              final List<Unit> selected =
                  CollectionUtils.getNMatches(selectFrom, count, Matches.unitIsStrategicBomber());
              return new CasualtyDetails(selected, List.of(), false);
            });
  }

  @BeforeEach
  void setUp() {
    final GameState data = TestMapGameData.REVISED.getGameData();
    bridge = newDelegateBridge(british(data));
  }

  @Test
  void testAaCasualtiesLowLuck() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, false);
    final DiceRoll roll = new DiceRoll(new int[] {0}, 1, 1, false, null);
    final Collection<Unit> planes = bomber(data).create(5, british(data));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    whenGetRandom(bridge).thenAnswer(withValues(0));
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(1, casualties.size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
  }

  @Test
  void testAaCasualtiesLowLuckDifferentMovementLeft() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, false);
    final DiceRoll roll = new DiceRoll(new int[] {0}, 1, 1, false, null);
    final List<Unit> planes = bomber(data).create(5, british(data));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    whenGetRandom(bridge).thenAnswer(withValues(0));
    planes.get(0).setAlreadyMoved(BigDecimal.ONE);
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(1, casualties.size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
  }

  @Test
  void testAaCasualtiesLowLuckMixed() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, false);
    // 6 bombers and 6 fighters
    final Collection<Unit> planes = bomber(data).create(6, british(data));
    planes.addAll(fighter(data).create(6, british(data)));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    // don't allow rolling, 6 of each is deterministic
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    CollectionUtils.getAny(defendingAa)
                        .getUnitAttachment()
                        .getTargetsAa(data.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", data),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", data).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(2, casualties.size());
    // should be 1 fighter and 1 bomber
    assertEquals(1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  void testAaCasualtiesLowLuckMixedMultipleDiceRolled() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, false);
    // 5 bombers and 5 fighters
    final Collection<Unit> planes = bomber(data).create(5, british(data));
    planes.addAll(fighter(data).create(5, british(data)));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    // should roll once, a hit
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(1, 1));
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    CollectionUtils.getAny(defendingAa)
                        .getUnitAttachment()
                        .getTargetsAa(data.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", data),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", data).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(2, casualties.size());
    // two extra rolls to pick which units are hit
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
    assertEquals(2, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        0, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
  }

  @Test
  void testAaCasualtiesLowLuckMixedWithChooseAaCasualties() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, true);
    // 6 bombers and 6 fighters
    final Collection<Unit> planes = bomber(data).create(6, british(data));
    planes.addAll(fighter(data).create(6, british(data)));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    givenRemotePlayerWillSelectStrategicBombersForCasualties();
    // don't allow rolling, 6 of each is deterministic
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    CollectionUtils.getAny(defendingAa)
                        .getUnitAttachment()
                        .getTargetsAa(data.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", data),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", data).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                british(data),
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(2, casualties.size());
    // we selected all bombers
    assertEquals(2, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        0, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  void testAaCasualtiesLowLuckMixedWithChooseAaCasualtiesRoll() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, true);
    // 7 bombers and 7 fighters
    final Collection<Unit> planes = bomber(data).create(7, british(data));
    planes.addAll(fighter(data).create(7, british(data)));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    givenRemotePlayerWillSelectStrategicBombersForCasualties();
    // only 1 roll, a hit
    whenGetRandom(bridge).thenAnswer(withValues(0));
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    CollectionUtils.getAny(defendingAa)
                        .getUnitAttachment()
                        .getTargetsAa(data.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", data),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", data).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                british(data),
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(3, casualties.size());
    // we selected all bombers
    assertEquals(3, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        0, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
  }

  @Test
  void testAaCasualtiesLowLuckMixedWithRolling() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, false);
    // 7 bombers and 7 fighters
    // 2 extra units, roll once
    final Collection<Unit> planes = bomber(data).create(7, british(data));
    planes.addAll(fighter(data).create(7, british(data)));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    // one roll, a hit
    whenGetRandom(bridge).thenAnswer(withValues(0)).thenAnswer(withValues(0));
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    CollectionUtils.getAny(defendingAa)
                        .getUnitAttachment()
                        .getTargetsAa(data.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", data),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", data).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    // make sure we rolled once
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(3, casualties.size());
    // a second roll for choosing which unit
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
    // should be 2 fighters and 1 bombers
    assertEquals(1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        2, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
  }

  @Test
  void testAaCasualtiesLowLuckMixedWithRollingMiss() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, false);
    // 7 bombers and 7 fighters
    // 2 extra units, roll once
    final Collection<Unit> planes = bomber(data).create(7, british(data));
    planes.addAll(fighter(data).create(7, british(data)));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    // one roll, a miss
    whenGetRandom(bridge)
        .thenAnswer(withValues(2))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0, 0));
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    CollectionUtils.getAny(defendingAa)
                        .getUnitAttachment()
                        .getTargetsAa(data.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", data),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", data).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    // make sure we rolled once
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(2, casualties.size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    // should be 1 fighter and 1 bomber
    assertEquals(1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
  }

  @Test
  void testAaCasualtiesLowLuckMixedWithRollingForBombers() {
    final GameData data = bridge.getData();
    makeGameLowLuck(data);
    setSelectAaCasualties(data, false);
    // 6 bombers, 7 fighters
    final Collection<Unit> planes = bomber(data).create(6, british(data));
    planes.addAll(fighter(data).create(7, british(data)));
    final Collection<Unit> defendingAa =
        territory("Germany", data).getUnitCollection().getMatches(Matches.unitIsAaForAnything());
    // 1 roll for the extra fighter
    whenGetRandom(bridge).thenAnswer(withValues(0));
    final DiceRoll roll =
        RollDiceFactory.rollAaDice(
            CollectionUtils.getMatches(
                planes,
                Matches.unitIsOfTypes(
                    CollectionUtils.getAny(defendingAa)
                        .getUnitAttachment()
                        .getTargetsAa(data.getUnitTypeList()))),
            defendingAa,
            bridge,
            territory("Germany", data),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(planes)
                .friendlyUnits(territory("Germany", data).getUnits())
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build());
    // make sure we rolled once
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
    final Collection<Unit> casualties =
        AaCasualtySelector.getAaCasualties(
                planes,
                defendingAa,
                CombatValueBuilder.mainCombatValue()
                    .enemyUnits(defendingAa)
                    .friendlyUnits(planes)
                    .side(BattleState.Side.OFFENSE)
                    .gameSequence(data.getSequence())
                    .supportAttachments(data.getUnitTypeList().getSupportRules())
                    .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                    .gameDiceSides(data.getDiceSides())
                    .territoryEffects(List.of())
                    .build(),
                CombatValueBuilder.aaCombatValue()
                    .enemyUnits(planes)
                    .friendlyUnits(defendingAa)
                    .side(BattleState.Side.DEFENSE)
                    .supportAttachments(data.getUnitTypeList().getSupportAaRules())
                    .build(),
                "",
                roll,
                bridge,
                null,
                null,
                territory("Germany", data))
            .getKilled();
    assertEquals(3, casualties.size());
    // should be 2 fighters and 1 bombers
    assertEquals(1, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber()));
    assertEquals(
        2, CollectionUtils.countMatches(casualties, Matches.unitIsStrategicBomber().negate()));
  }
  // Radar AA tests removed, because "revised" does not have radar tech.
}
