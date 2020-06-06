package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanNotBeTargetedBy;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static games.strategy.triplea.delegate.battle.steps.MockStepParameters.givenStepParameters;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.StepParameters;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AirDefendVsNonSubsStepTest {

  @Test
  @DisplayName("valid() is true if defender has air and no destroyer and attacker has sub")
  void airVsSub() {
    final Unit defender1 = givenUnit();
    final Unit defender2 = givenUnitIsAir();
    final Unit attacker = givenUnitCanNotBeTargetedBy(mock(UnitType.class));

    final StepParameters parameters =
        givenStepParameters()
            .attackingUnits(List.of(defender1, defender2))
            .defendingUnits(List.of(attacker))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.NAME), is(true));
  }

  @Test
  @DisplayName("valid() is false if defender has air and destroyer")
  void airDestroyerVsAnything() {
    final Unit defender1 = givenUnitDestroyer();
    final Unit defender2 = givenUnitIsAir();
    // once a destroyer is around, it doesn't care about the attacker units
    final Unit attacker = mock(Unit.class);

    final StepParameters parameters =
        givenStepParameters()
            .attackingUnits(List.of(defender1, defender2))
            .defendingUnits(List.of(attacker))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.NAME), is(false));
  }

  @Test
  @DisplayName("valid() is false if defender has air and no destroyer and attacker has no sub")
  void airVsOther() {
    final Unit defender1 = givenUnit();
    final Unit defender2 = givenUnitIsAir();
    final Unit attacker = givenUnit();

    final StepParameters parameters =
        givenStepParameters()
            .attackingUnits(List.of(defender1, defender2))
            .defendingUnits(List.of(attacker))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.NAME), is(false));
  }
}
