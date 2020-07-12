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
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.FirstStrikeResult;
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
    if (parameters.contains(BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK)) {
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

    final FirstStrikeResult steps = FirstStrikeStepOrder.calculate(battleState);

    assertThat(steps, is(FirstStrikeResult.builder().build()));
  }

  @ParameterizedTest
  @MethodSource
  void getStep(final List<BattleStateVariation> parameters, final FirstStrikeResult expected) {

    final BattleState battleState = givenBattleState(parameters);

    final FirstStrikeResult order = FirstStrikeStepOrder.calculate(battleState);

    assertThat(order, is(expected));
  }

  @SuppressWarnings("unused")
  static List<Arguments> getStep() {
    return List.of(
        Arguments.of(
            List.of(BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE),
            FirstStrikeResult.builder().attacker(OFFENDER_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            FirstStrikeResult.builder().attacker(OFFENDER_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            FirstStrikeResult.builder().attacker(OFFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            FirstStrikeResult.builder().attacker(OFFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder().attacker(OFFENDER_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder().attacker(OFFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder().attacker(OFFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder().attacker(OFFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder().attacker(OFFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder().attacker(OFFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder().attacker(OFFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE),
            FirstStrikeResult.builder().defender(DEFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            FirstStrikeResult.builder().defender(DEFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            FirstStrikeResult.builder().defender(DEFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE, BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            FirstStrikeResult.builder().defender(DEFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .defender(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder().defender(DEFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder().defender(DEFENDER_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .defender(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder().defender(DEFENDER_NO_SNEAK_ATTACK).build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .defender(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK)
                .defender(DEFENDER_NO_SNEAK_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK)
                .defender(DEFENDER_NO_SNEAK_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_NO_SNEAK_ATTACK)
                .defender(DEFENDER_NO_SNEAK_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_NO_SNEAK_ATTACK)
                .defender(DEFENDER_NO_SNEAK_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .defender(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK)
                .defender(DEFENDER_NO_SNEAK_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_NO_SNEAK_ATTACK)
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_NO_SNEAK_ATTACK)
                .defender(DEFENDER_SNEAK_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_NO_SNEAK_ATTACK)
                .defender(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_NO_SNEAK_ATTACK)
                .defender(DEFENDER_NO_SNEAK_ATTACK)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_NO_SNEAK_ATTACK)
                .defender(DEFENDER_SNEAK_ATTACK_WITH_OPPOSING_FIRST_STRIKE)
                .build()),
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK),
            FirstStrikeResult.builder()
                .attacker(OFFENDER_NO_SNEAK_ATTACK)
                .defender(DEFENDER_NO_SNEAK_ATTACK_BUT_BEFORE_STANDARD_ATTACK)
                .build()));
  }
}
