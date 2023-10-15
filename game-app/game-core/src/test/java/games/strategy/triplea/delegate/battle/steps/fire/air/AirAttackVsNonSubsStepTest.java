package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenAnyUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenSeaUnitCanNotBeTargetedBy;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AirAttackVsNonSubsStepTest {

  @ParameterizedTest(name = "[{index}] {0} is {2}")
  @MethodSource
  void stepName(final String displayName, final BattleState battleState, final boolean expected) {
    final AirAttackVsNonSubsStep airAttackVsNonSubsStep = new AirAttackVsNonSubsStep(battleState);
    assertThat(airAttackVsNonSubsStep.getAllStepDetails(), hasSize(expected ? 1 : 0));
  }

  static List<Arguments> stepName() {
    return List.of(
        Arguments.of(
            "Attacker has air units and no destroyers vs Defender subs",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenAnyUnit(), givenUnitIsAir()))
                .defendingUnits(List.of(givenSeaUnitCanNotBeTargetedBy(mock(UnitType.class))))
                .build(),
            true),
        Arguments.of(
            "Attacker has air units and destroyers",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenUnitDestroyer(), givenUnitIsAir()))
                // once a destroyer is around, it doesn't care about whether a sub exists or not
                .defendingUnits(List.of(mock(Unit.class)))
                .build(),
            false),
        Arguments.of(
            "Attacker has air units but no destroyers vs Defender with no subs",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenAnyUnit(), givenUnitIsAir()))
                .defendingUnits(List.of(givenAnyUnit()))
                .build(),
            false));
  }
}
