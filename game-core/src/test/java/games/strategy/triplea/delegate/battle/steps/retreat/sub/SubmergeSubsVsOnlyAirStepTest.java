package games.strategy.triplea.delegate.battle.steps.retreat.sub;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvadeAndCanNotBeTargetedByRandomUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmergeSubsVsOnlyAirStepTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @ParameterizedTest(name = "[{index}] {0} is {2}")
  @MethodSource
  void stepName(final String displayName, final BattleState battleState, final boolean expected) {
    final SubmergeSubsVsOnlyAirStep submergeSubsVsOnlyAirStep =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    assertThat(submergeSubsVsOnlyAirStep.getNames(), hasSize(expected ? 1 : 0));
  }

  static List<Arguments> stepName() {
    return List.of(
        Arguments.of(
            "Attacking evaders vs NO air",
            givenBattleStateBuilder()
                // if there is no air, it doesn't check if the attacker is an evader
                .attackingUnits(List.of(givenAnyUnit()))
                .defendingUnits(List.of(givenAnyUnit(), mock(Unit.class)))
                .build(),
            false),
        Arguments.of(
            "Defending evaders vs NO air",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenAnyUnit(), mock(Unit.class)))
                // if there is no air, it doesn't check if the defender is an evader
                .defendingUnits(List.of(givenAnyUnit()))
                .build(),
            false),
        Arguments.of(
            "Attacking evaders vs SOME air",
            givenBattleStateBuilder()
                // if there is some but not all air, it doesn't check if the attacker is an evader
                .attackingUnits(List.of(givenAnyUnit()))
                .defendingUnits(List.of(givenUnitIsAir(), givenAnyUnit()))
                .build(),
            false),
        Arguments.of(
            "Defending evaders vs SOME air",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitIsAir(), givenAnyUnit()))
                // if there is some but not all air, it doesn't check if the defender is an evader
                .defendingUnits(List.of(givenAnyUnit()))
                .build(),
            false),
        Arguments.of(
            "Attacking evaders vs ALL air",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitCanEvadeAndCanNotBeTargetedByRandomUnit()))
                .defendingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .build(),
            true),
        Arguments.of(
            "Defending evaders vs ALL air",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .defendingUnits(List.of(givenUnitCanEvadeAndCanNotBeTargetedByRandomUnit()))
                .build(),
            true));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource
  void submergeUnits(
      final String displayName,
      final BattleState battleState,
      final List<Unit> expectedSubmergingSubs,
      final boolean expectedSide) {
    final SubmergeSubsVsOnlyAirStep submergeSubsVsOnlyAirStep =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);

    submergeSubsVsOnlyAirStep.execute(executionStack, delegateBridge);

    verify(battleActions).submergeUnits(expectedSubmergingSubs, expectedSide, delegateBridge);
  }

  static List<Arguments> submergeUnits() {
    final Unit sub = givenUnitCanEvadeAndCanNotBeTargetedByRandomUnit();
    return List.of(
        Arguments.of(
            "Attacking subs submerge",
            givenBattleStateBuilder()
                .attackingUnits(List.of(sub, givenAnyUnit()))
                .defendingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .build(),
            List.of(sub),
            false),
        Arguments.of(
            "Defending subs submerge",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitIsAir(), givenUnitIsAir()))
                .defendingUnits(List.of(sub, givenAnyUnit()))
                .build(),
            List.of(sub),
            true));
  }
}
