package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.MockBattleState.givenBattleState;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnit;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitCanNotBeTargetedBy;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitDestroyer;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitIsAir;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AirAttackVsNonSubsStepTest {

  @Test
  @DisplayName("valid() is true if attacker has air and no destroyer and defender has sub")
  void airVsSub() {
    final Unit attacker1 = givenUnit();
    final Unit attacker2 = givenUnitIsAir();
    final Unit defender = givenUnitCanNotBeTargetedBy(mock(UnitType.class));

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(battleState);
    assertThat(underTest.valid(), is(true));
  }

  @Test
  @DisplayName("valid() is false if attacker has air and destroyer")
  void airDestroyerVsAnything() {
    final Unit attacker1 = givenUnitDestroyer();
    final Unit attacker2 = givenUnitIsAir();
    // once a destroyer is around, it doesn't care about the defender units
    final Unit defender = mock(Unit.class);

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(battleState);
    assertThat(underTest.valid(), is(false));
  }

  @Test
  @DisplayName("valid() is false if attacker has air and no destroyer and defender has no sub")
  void airVsOther() {
    final Unit attacker1 = givenUnit();
    final Unit attacker2 = givenUnitIsAir();
    final Unit defender = givenUnit();

    final BattleState battleState =
        givenBattleState()
            .attackingUnits(List.of(attacker1, attacker2))
            .defendingUnits(List.of(defender))
            .build();
    final AirAttackVsNonSubsStep underTest = new AirAttackVsNonSubsStep(battleState);
    assertThat(underTest.valid(), is(false));
  }
}
