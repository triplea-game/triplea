package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvadeAndCanNotBeTargetedBy;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.battle.BattleActions;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class SubmergeSubsVsOnlyAirStepTest {

  @Mock BattleActions battleActions;
  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;

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
  @DisplayName("valid() is false given some attacking evaders vs NO air")
  void attackingEvadersVsNoAirIsValidPreRound() {

    final Unit attacker = givenUnit();
    final Unit defender1 = givenUnit();
    final Unit defender2 = mock(Unit.class);

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest = new SubmergeSubsVsOnlyAirStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.NAME), is(false));
  }

  @Test
  @DisplayName("valid() is true given some defending evaders vs NO air")
  void defendingEvadersVsNoAirIsValidPreRound() {

    final Unit attacker1 = givenUnit();
    final Unit attacker2 = mock(Unit.class);
    final Unit defender = givenUnit();

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest = new SubmergeSubsVsOnlyAirStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.NAME), is(false));
  }

  @Test
  @DisplayName("valid() is false given some attacking evaders vs SOME air")
  void attackingEvadersVsSomeAirIsNotValidInRound() {

    final Unit attacker = givenUnit();
    final Unit defender1 = givenUnitIsAir();
    final Unit defender2 = givenUnit();

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest = new SubmergeSubsVsOnlyAirStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.EXEC), is(false));
  }

  @Test
  @DisplayName("valid() is true given some defending evaders vs SOME air")
  void defendingEvadersVsSomeAirIsNotValidInRound() {

    final Unit attacker1 = givenUnitIsAir();
    final Unit attacker2 = givenUnit();
    final Unit defender = givenUnit();

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest = new SubmergeSubsVsOnlyAirStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.EXEC), is(false));
  }

  @Test
  @DisplayName("valid(IN_ROUND) is false given some attacking evaders vs ALL air")
  void attackingEvadersVsAllAirIsValidInRound() {

    final Unit attacker = givenUnitCanEvadeAndCanNotBeTargetedBy(mock(UnitType.class));
    final Unit defender1 = givenUnitIsAir();
    final Unit defender2 = givenUnitIsAir();

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest = new SubmergeSubsVsOnlyAirStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.EXEC), is(true));
  }

  @Test
  @DisplayName("valid(IN_ROUND) is true given some defending evaders vs ALL air")
  void defendingEvadersVsAllAirIsValidInRound() {

    final Unit attacker1 = givenUnitIsAir();
    final Unit attacker2 = givenUnitIsAir();
    final Unit defender = givenUnitCanEvadeAndCanNotBeTargetedBy(mock(UnitType.class));

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest = new SubmergeSubsVsOnlyAirStep(parameters);
    assertThat(underTest.valid(BattleStep.Request.EXEC), is(true));
  }

  @Test
  @DisplayName("Submerge attacking evaders vs ALL air")
  void attackingEvadersSubmergeVsAllAir() {

    final Unit attacker1 = givenUnitCanEvadeAndCanNotBeTargetedBy(mock(UnitType.class));
    final Unit attacker2 = givenUnit();
    final Unit defender1 = givenUnitIsAir();
    final Unit defender2 = givenUnitIsAir();

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest = new SubmergeSubsVsOnlyAirStep(parameters);
    final IExecutable step = underTest.getExecutable();

    step.execute(executionStack, delegateBridge);

    verify(battleActions).submergeUnits(List.of(attacker1), false, delegateBridge);
  }

  @Test
  @DisplayName("Submerge defending evaders vs ALL air")
  void defendingEvadersSubmergeVsAllAir() {

    final Unit defender1 = givenUnitCanEvadeAndCanNotBeTargetedBy(mock(UnitType.class));
    final Unit defender2 = givenUnit();
    final Unit attacker1 = givenUnitIsAir();
    final Unit attacker2 = givenUnitIsAir();

    final StepParameters parameters =
        givenParameters()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest = new SubmergeSubsVsOnlyAirStep(parameters);
    final IExecutable step = underTest.getExecutable();

    step.execute(executionStack, delegateBridge);

    verify(battleActions).submergeUnits(List.of(defender1), true, delegateBridge);
  }
}
