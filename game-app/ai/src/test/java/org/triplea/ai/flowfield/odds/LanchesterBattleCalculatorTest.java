package org.triplea.ai.flowfield.odds;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class LanchesterBattleCalculatorTest {
  private static final double GLOBAL1940_ATTRITION_ORDER = 1.50;

  private static final GameData gameData = TestMapGameData.GLOBAL1940.getGameData();
  private static final UnitType infantry = GameDataTestUtil.infantry(gameData);
  private static final UnitType mechInfantry = GameDataTestUtil.mechInfantry(gameData);
  private static final UnitType artillery = GameDataTestUtil.artillery(gameData);
  private static final GamePlayer british = GameDataTestUtil.british(gameData);
  private static final GamePlayer germans = GameDataTestUtil.germans(gameData);

  @Test
  void infantryVsInfantry() {
    final Collection<Unit> britishUnits = infantry.create(1, british);
    final Collection<Unit> germanUnits = infantry.create(1, germans);
    final LanchesterBattleCalculator calculator =
        new LanchesterBattleCalculator(
            PowerStrengthAndRolls.build(
                britishUnits,
                CombatValueBuilder.mainCombatValue()
                    .friendlyUnits(britishUnits)
                    .enemyUnits(germanUnits)
                    .side(OFFENSE)
                    .territoryEffects(List.of())
                    .gameDiceSides(gameData.getDiceSides())
                    .gameSequence(gameData.getSequence())
                    .lhtrHeavyBombers(false)
                    .supportAttachments(List.of())
                    .build()),
            PowerStrengthAndRolls.build(
                germanUnits,
                CombatValueBuilder.mainCombatValue()
                    .friendlyUnits(germanUnits)
                    .enemyUnits(britishUnits)
                    .side(DEFENSE)
                    .territoryEffects(List.of())
                    .gameDiceSides(gameData.getDiceSides())
                    .gameSequence(gameData.getSequence())
                    .lhtrHeavyBombers(false)
                    .supportAttachments(List.of())
                    .build()),
            GLOBAL1940_ATTRITION_ORDER);

    assertThat(calculator.getWon(), is(DEFENSE));
    assertThat(calculator.getRemainingUnits(), is(1L));
  }

  @Test
  void threeInfantryVsInfantry() {
    final Collection<Unit> britishUnits = infantry.create(3, british);
    final Collection<Unit> germanUnits = infantry.create(1, germans);
    final LanchesterBattleCalculator calculator =
        new LanchesterBattleCalculator(
            PowerStrengthAndRolls.build(
                britishUnits,
                CombatValueBuilder.mainCombatValue()
                    .friendlyUnits(britishUnits)
                    .enemyUnits(germanUnits)
                    .side(OFFENSE)
                    .territoryEffects(List.of())
                    .gameDiceSides(gameData.getDiceSides())
                    .gameSequence(gameData.getSequence())
                    .lhtrHeavyBombers(false)
                    .supportAttachments(List.of())
                    .build()),
            PowerStrengthAndRolls.build(
                germanUnits,
                CombatValueBuilder.mainCombatValue()
                    .friendlyUnits(germanUnits)
                    .enemyUnits(britishUnits)
                    .side(DEFENSE)
                    .territoryEffects(List.of())
                    .gameDiceSides(gameData.getDiceSides())
                    .gameSequence(gameData.getSequence())
                    .lhtrHeavyBombers(false)
                    .supportAttachments(List.of())
                    .build()),
            GLOBAL1940_ATTRITION_ORDER);

    assertThat(calculator.getWon(), is(OFFENSE));
    assertThat(calculator.getRemainingUnits(), is(2L));
  }

  @Test
  void infantryAndMechInfantryVsArtilleryAndInfantry() {
    final Collection<Unit> britishUnits = infantry.create(3, british);
    britishUnits.addAll(mechInfantry.create(5, british));
    final Collection<Unit> germanUnits = artillery.create(6, germans);
    germanUnits.addAll(infantry.create(9, germans));
    final LanchesterBattleCalculator calculator =
        new LanchesterBattleCalculator(
            PowerStrengthAndRolls.build(
                britishUnits,
                CombatValueBuilder.mainCombatValue()
                    .friendlyUnits(britishUnits)
                    .enemyUnits(germanUnits)
                    .side(OFFENSE)
                    .territoryEffects(List.of())
                    .gameDiceSides(gameData.getDiceSides())
                    .gameSequence(gameData.getSequence())
                    .lhtrHeavyBombers(false)
                    .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                    .build()),
            PowerStrengthAndRolls.build(
                germanUnits,
                CombatValueBuilder.mainCombatValue()
                    .friendlyUnits(germanUnits)
                    .enemyUnits(britishUnits)
                    .side(DEFENSE)
                    .territoryEffects(List.of())
                    .gameDiceSides(gameData.getDiceSides())
                    .gameSequence(gameData.getSequence())
                    .lhtrHeavyBombers(false)
                    .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                    .build()),
            GLOBAL1940_ATTRITION_ORDER);

    assertThat(calculator.getWon(), is(DEFENSE));
    assertThat(calculator.getRemainingUnits(), is(13L));
  }
}
