package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanNotBeTargetedBy;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleActions;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class AirAttackVsNonSubsStepTest {

  @Mock BattleActions battleActions;

  private StepParameters.StepParametersBuilder givenParameters() {
    final StepParameters.StepParametersBuilder builder =
        StepParameters.builder()
            .attackingUnits(List.of())
            .defendingUnits(List.of())
            .battleActions(battleActions);

    // run build when it is called so that changes to the parameters in the test methods will take
    // affect
    lenient()
        .when(battleActions.getStepParameters())
        .then((Answer<StepParameters>) invocation -> builder.build());
    return builder;
  }

  @Test
  @DisplayName("valid() is true if attacker has air and no destroyer and defender has sub")
  void airVsSub() {
    final Unit attacker1 = givenUnit();
    final Unit attacker2 = givenUnitIsAir();
    final Unit defender = givenUnitCanNotBeTargetedBy(mock(UnitType.class));

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(parameters);
    assertThat(underTest.valid(BattleStep.State.PRE_ROUND), is(true));
  }

  @Test
  @DisplayName("valid() is false if attacker has air and destroyer")
  void airDestroyerVsAnything() {
    final Unit attacker1 = givenUnitDestroyer();
    final Unit attacker2 = givenUnitIsAir();
    // once a destroyer is around, it doesn't care about the defender units
    final Unit defender = mock(Unit.class);

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(parameters);
    assertThat(underTest.valid(BattleStep.State.PRE_ROUND), is(false));
  }

  @Test
  @DisplayName("valid() is false if attacker has air and no destroyer and defender has no sub")
  void airVsOther() {
    final Unit attacker1 = givenUnit();
    final Unit attacker2 = givenUnitIsAir();
    final Unit defender = givenUnit();

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(parameters);
    assertThat(underTest.valid(BattleStep.State.PRE_ROUND), is(false));
  }
}
