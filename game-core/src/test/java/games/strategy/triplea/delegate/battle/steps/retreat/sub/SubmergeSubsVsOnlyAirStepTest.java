package games.strategy.triplea.delegate.battle.steps.retreat.sub;

import static games.strategy.triplea.delegate.battle.MockBattleState.givenBattleState;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Request;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanEvadeAndCanNotBeTargetedBy;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmergeSubsVsOnlyAirStepTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  @DisplayName("valid() is false given some attacking evaders vs NO air")
  void attackingEvadersVsNoAirIsValidPreRound() {

    final Unit attacker = givenUnit();
    final Unit defender1 = givenUnit();
    final Unit defender2 = mock(Unit.class);

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    assertThat(underTest.valid(Request.NAME), is(false));
  }

  @Test
  @DisplayName("valid() is true given some defending evaders vs NO air")
  void defendingEvadersVsNoAirIsValidPreRound() {

    final Unit attacker1 = givenUnit();
    final Unit attacker2 = mock(Unit.class);
    final Unit defender = givenUnit();

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    assertThat(underTest.valid(Request.NAME), is(false));
  }

  @Test
  @DisplayName("valid() is false given some attacking evaders vs SOME air")
  void attackingEvadersVsSomeAirIsNotValidInRound() {

    final Unit attacker = givenUnit();
    final Unit defender1 = givenUnitIsAir();
    final Unit defender2 = givenUnit();

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    assertThat(underTest.valid(Request.EXEC), is(false));
  }

  @Test
  @DisplayName("valid() is true given some defending evaders vs SOME air")
  void defendingEvadersVsSomeAirIsNotValidInRound() {

    final Unit attacker1 = givenUnitIsAir();
    final Unit attacker2 = givenUnit();
    final Unit defender = givenUnit();

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    assertThat(underTest.valid(Request.EXEC), is(false));
  }

  @Test
  @DisplayName("valid(IN_ROUND) is false given some attacking evaders vs ALL air")
  void attackingEvadersVsAllAirIsValidInRound() {

    final Unit attacker = givenUnitCanEvadeAndCanNotBeTargetedBy(mock(UnitType.class));
    final Unit defender1 = givenUnitIsAir();
    final Unit defender2 = givenUnitIsAir();

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    assertThat(underTest.valid(Request.EXEC), is(true));
  }

  @Test
  @DisplayName("valid(IN_ROUND) is true given some defending evaders vs ALL air")
  void defendingEvadersVsAllAirIsValidInRound() {

    final Unit attacker1 = givenUnitIsAir();
    final Unit attacker2 = givenUnitIsAir();
    final Unit defender = givenUnitCanEvadeAndCanNotBeTargetedBy(mock(UnitType.class));

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    assertThat(underTest.valid(Request.EXEC), is(true));
  }

  @Test
  @DisplayName("Submerge attacking evaders vs ALL air")
  void attackingEvadersSubmergeVsAllAir() {

    final Unit attacker1 = givenUnitCanEvadeAndCanNotBeTargetedBy(mock(UnitType.class));
    final Unit attacker2 = givenUnit();
    final Unit defender1 = givenUnitIsAir();
    final Unit defender2 = givenUnitIsAir();

    final BattleActions battleActions = mock(BattleActions.class);

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
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

    final BattleActions battleActions = mock(BattleActions.class);

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender1, defender2))
            .build();
    final SubmergeSubsVsOnlyAirStep underTest =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    final IExecutable step = underTest.getExecutable();

    step.execute(executionStack, delegateBridge);

    verify(battleActions).submergeUnits(List.of(defender1), true, delegateBridge);
  }
}
