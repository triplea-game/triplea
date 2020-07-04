package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitFirstStrike;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.FIRST_STRIKE_DEFENDER_FIRST_NONE;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.FIRST_STRIKE_DEFENDER_SECOND_ALL;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.FIRST_STRIKE_DEFENDER_SECOND_SUBS;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.FIRST_STRIKE_DEFENDER_STANDARD_ALL;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.FIRST_STRIKE_OFFENDER_ALL;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.FIRST_STRIKE_OFFENDER_NONE;
import static games.strategy.triplea.delegate.battle.steps.FirstStrikeStepOrder.FIRST_STRIKE_OFFENDER_SUBS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreatTest.MockGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FirstStrikeStepOrderTest {

  @ParameterizedTest
  @MethodSource
  void getStep(final List<BattleStateVariation> parameters, final List<FirstStrikeStepOrder> expected, final List<FirstStrikeStepOrder> expected2) {

    final BattleState battleState = givenBattleState(parameters);

    final List<FirstStrikeStepOrder> steps = FirstStrikeStepOrder.calculate(battleState);

    if (expected.isEmpty()) {
      assertThat(steps, hasSize(0));
    } else {
      System.out.println(steps);
      assertThat(steps, contains(expected.toArray()));
    }

    final List<FirstStrikeStepOrder> steps2 = FirstStrikeStepOrder.calculate2(battleState);
    if (expected2.isEmpty()) {
      assertThat(steps2, hasSize(0));
    } else {
      System.out.println(steps2);
      assertThat(steps2, contains(expected2.toArray()));
    }
  }

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

  static List<Arguments> getStep() {
    return List.of(
        //1
        Arguments.of(
            List.of(),
            List.of(),
        List.of()),
        //2
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE
            ),
            List.of(FIRST_STRIKE_OFFENDER_NONE),
        List.of(FIRST_STRIKE_OFFENDER_NONE)),
        //3
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER
            ),
            List.of(FIRST_STRIKE_OFFENDER_NONE),
        List.of(FIRST_STRIKE_OFFENDER_NONE)),
        //4
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL)),
        //5
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_OFFENDER_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_SUBS)),
        //6
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_SUBS)),
        //7
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL)),
        //8
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_OFFENDER_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_SUBS)),
        //9
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_NONE),
        List.of(FIRST_STRIKE_OFFENDER_NONE)),
        //10
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL)),
        //11
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL)),
        //12
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_SUBS)),
        //13
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL)),
        //14
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL)),
        //15
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL)),
        //16
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL)),

        //17
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE
            ),
            List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //18
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER
            ),
            List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //19
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER
            ),
            List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //20
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //21
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //22
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER
            ),
            List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //23
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_DEFENDER_SECOND_ALL),
        List.of(FIRST_STRIKE_DEFENDER_SECOND_ALL)),
        //24
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //25
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //26
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_DEFENDER_FIRST_NONE),
        List.of(FIRST_STRIKE_DEFENDER_FIRST_NONE)),
        //27
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //28
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_DEFENDER_SECOND_ALL),
        List.of(FIRST_STRIKE_DEFENDER_SECOND_ALL)),
        //29
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //30
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //31
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_DEFENDER_SECOND_ALL),
        List.of(FIRST_STRIKE_DEFENDER_SECOND_ALL)),

        //32
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE
            ),
            List.of(FIRST_STRIKE_OFFENDER_NONE, FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_OFFENDER_NONE, FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //33
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER
            ),
            List.of(FIRST_STRIKE_OFFENDER_NONE, FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_OFFENDER_NONE, FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //34
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //35
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_OFFENDER_SUBS, FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_SUBS, FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //36
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_SUBS, FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_SUBS, FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //37
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //38
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_OFFENDER_SUBS, FIRST_STRIKE_DEFENDER_SECOND_ALL),
        List.of(FIRST_STRIKE_OFFENDER_SUBS, FIRST_STRIKE_DEFENDER_SECOND_ALL)),
        //39
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_NONE, FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_OFFENDER_NONE, FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //40
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //41
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_DEFENDER_FIRST_NONE, FIRST_STRIKE_OFFENDER_ALL),
        List.of(FIRST_STRIKE_DEFENDER_FIRST_NONE, FIRST_STRIKE_OFFENDER_ALL)),
        //42
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_SUBS, FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_SUBS, FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //43
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_SECOND_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_SECOND_ALL)),
        //44
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_STANDARD_ALL),
        List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_STANDARD_ALL)),
        //45
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_SECOND_SUBS),
        List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_SECOND_SUBS)),
        //46
        Arguments.of(
            List.of(
                BattleStateVariation.HAS_ATTACKING_FIRST_STRIKE,
                BattleStateVariation.HAS_DEFENDING_FIRST_STRIKE,
                BattleStateVariation.HAS_ATTACKING_DESTROYER,
                BattleStateVariation.HAS_DEFENDING_DESTROYER,
                BattleStateVariation.HAS_WW2V2,
                BattleStateVariation.HAS_DEFENDING_SUBS_SNEAK_ATTACK
            ),
            List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_SECOND_ALL),
            List.of(FIRST_STRIKE_OFFENDER_ALL, FIRST_STRIKE_DEFENDER_SECOND_ALL))
    );
  }
}