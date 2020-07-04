package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.DEFENDER_NO_SNEAK_ATTACK;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.DEFENDER_SNEAK_ATTACK;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.OFFENDER_NO_SNEAK_ATTACK;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.OFFENDER_SNEAK_ATTACK;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreatTest.MockGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FirstStrikeStepOrderTest {

  enum BattleStateVariation {
    HAS_ATTACKING_FIRST_STRIKE,
    HAS_DEFENDING_FIRST_STRIKE,
    HAS_ATTACKING_DESTROYER,
    HAS_DEFENDING_DESTROYER,
    HAS_WW2V2,
    HAS_DEFENDING_SUBS_SNEAK_ATTACK
  }

  BattleState givenBattleState(final List<BattleStateVariation> parameters) {
    final List<Unit> attackingUnits = new ArrayList<>();
    attackingUnits.add(givenAnyUnit());
    if (parameters.contains(BattleStateVariation.HAS_ATTACKING_DESTROYER)) {
      attackingUnits.add(givenUnitDestroyer());
    }
    if (parameters.contains(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE)) {
      attackingUnits.add(givenUnitFirstStrike());
    }
    final List<Unit> defendingUnits = new ArrayList<>();
    defendingUnits.add(givenAnyUnit());
    if (parameters.contains(BattleStateVariation.HAS_DEFENDING_DESTROYER)) {
      defendingUnits.add(givenUnitDestroyer());
    }
    if (parameters.contains(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE)) {
      defendingUnits.add(givenUnitFirstStrike());
    }
    final MockGameData gameData = MockGameData.givenGameData();

    if (parameters.contains(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE)
        && !parameters.contains(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE)) {
      gameData.withDefendingSuicideAndMunitionUnitsDoNotFire(false);
    }

    if (parameters.contains(BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK)) {
      if (!parameters.contains(BattleStateVariation.HAS_WW2V2)) {
        gameData.withWW2V2(false);
      }
      gameData.withDefendingSubsSneakAttack(true);
    }
    if (parameters.contains(BattleStateVariation.HAS_WW2V2)) {
      gameData.withWW2V2(true);
    }

    return givenBattleStateBuilder()
        .attackingUnits(attackingUnits)
        .defendingUnits(defendingUnits)
        .gameData(gameData.build())
        .build();
  }

  @Test
  void noFirstStrikeUnitsShouldReturnNothing() {
    final BattleState battleState = givenBattleState(List.of());

    final List<FirstStrikeStepOrder> steps = FirstStrikeStepOrder.calculate(battleState);

    assertThat(steps, hasSize(0));
  }

  @ParameterizedTest
  @MethodSource
  void getStep(
      final List<BattleStateVariation> parameters, final List<FirstStrikeStepOrder> expected) {

    final BattleState battleState = givenBattleState(parameters);

    final List<FirstStrikeStepOrder> steps = FirstStrikeStepOrder.calculate(battleState);

    assertThat(steps, containsInAnyOrder(expected.toArray()));
  }

  @SuppressWarnings("unused")
  static List<Arguments> getStep() {
    return List.of(
        Arguments.of(
            List.of(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE),
            List.of(OFFENDER_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            List.of(OFFENDER_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of(OFFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            List.of(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of(OFFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(OFFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(OFFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE),
            List.of(DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            List.of(DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of(DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            List.of(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of(DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(DEFENDER_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE),
            List.of(OFFENDER_SNEAK_ATTACK, DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            List.of(OFFENDER_SNEAK_ATTACK, DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of(OFFENDER_NO_SNEAK_ATTACK, DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2),
            List.of(
                OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE,
                DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(
                OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE,
                DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            List.of(OFFENDER_NO_SNEAK_ATTACK, DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(
                OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE,
                DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_SNEAK_ATTACK, DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(OFFENDER_NO_SNEAK_ATTACK, DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_NO_SNEAK_ATTACK, DEFENDER_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(
                OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE,
                DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            List.of(OFFENDER_NO_SNEAK_ATTACK, DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_NO_SNEAK_ATTACK, DEFENDER_NO_SNEAK_ATTACK)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(OFFENDER_NO_SNEAK_ATTACK, DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            List.of(
                OFFENDER_NO_SNEAK_ATTACK, DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)));
  }
}
