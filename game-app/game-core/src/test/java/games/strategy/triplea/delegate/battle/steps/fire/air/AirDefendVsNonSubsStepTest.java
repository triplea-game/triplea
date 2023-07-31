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
class AirDefendVsNonSubsStepTest {

  @ParameterizedTest(name = "[{index}] {0} is {2}")
  @MethodSource
  void stepName(final String displayName, final BattleState battleState, final boolean expected) {
    final AirDefendVsNonSubsStep airDefendVsNonSubsStep = new AirDefendVsNonSubsStep(battleState);
    assertThat(airDefendVsNonSubsStep.getAllStepDetails(), hasSize(expected ? 1 : 0));
  }

  static List<Arguments> stepName() {
    return List.of(
        Arguments.of(
            "Defender has air units and no destroyers vs Attacker subs",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenSeaUnitCanNotBeTargetedBy(mock(UnitType.class))))
                .defendingUnits(List.of(givenAnyUnit(), givenUnitIsAir()))
                .build(),
            true),
        Arguments.of(
            "Defender has air units and destroyers",
            givenBattleStateBuilder()
                // once a destroyer is around, it doesn't care about whether a sub exists or not
                .attackingUnits(List.of(mock(Unit.class)))
                .defendingUnits(List.of(givenUnitDestroyer(), givenUnitIsAir()))
                .build(),
            false),
        Arguments.of(
            "Defender has air units but no destroyers vs Attacker with no subs",
            givenBattleStateBuilder()
                .attackingUnits(List.of(givenAnyUnit()))
                .defendingUnits(List.of(givenAnyUnit(), givenUnitIsAir()))
                .build(),
            false));
  }
}
