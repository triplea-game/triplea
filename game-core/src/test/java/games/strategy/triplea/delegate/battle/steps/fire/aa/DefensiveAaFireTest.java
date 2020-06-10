package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleStepsTest.givenUnitWithTypeAa;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefensiveAaFireTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Nested
  class IsValid {
    @Test
    void validIfAaIsAvailable() {
      final BattleState battleState = givenBattleStateBuilder().defendingAa(List.of(mock(Unit.class))).build();
      final DefensiveAaFire defensiveAaFire = new DefensiveAaFire(battleState, battleActions);
      assertThat(defensiveAaFire.valid(), is(true));
    }

    @Test
    void notValidIfNoAaIsAvailable() {
      final BattleState battleState = givenBattleStateBuilder().defendingAa(List.of()).build();
      final DefensiveAaFire defensiveAaFire = new DefensiveAaFire(battleState, battleActions);
      assertThat(defensiveAaFire.valid(), is(false));
    }
  }

  @Nested
  class GetNames {
    @Test
    void hasNamesIfAaIsAvailable() {
      final BattleState battleState = givenBattleStateBuilder().defendingAa(List.of(givenUnitWithTypeAa())).build();
      final DefensiveAaFire defensiveAaFire = new DefensiveAaFire(battleState, battleActions);
      assertThat(defensiveAaFire.getNames(), hasSize(3));
    }

    @Test
    void hasNoNamesIfNoAaIsAvailable() {
      final BattleState battleState = givenBattleStateBuilder().defendingAa(List.of()).build();
      final DefensiveAaFire defensiveAaFire = new DefensiveAaFire(battleState, battleActions);
      assertThat(defensiveAaFire.getNames(), hasSize(0));
    }
  }

  @Nested
  class FireAa {
    @Test
    void firedIfAaAreAvailable() {
      final DefensiveAaFire defensiveAaFire =
          new DefensiveAaFire(
              givenBattleStateBuilder().defendingAa(List.of(mock(Unit.class))).build(),
              battleActions);

      defensiveAaFire.execute(executionStack, delegateBridge);

      verify(battleActions).fireDefensiveAaGuns();
    }

    @Test
    void notFiredIfNoAaAreAvailable() {
      final DefensiveAaFire defensiveAaFire =
          new DefensiveAaFire(
              givenBattleStateBuilder().defendingAa(List.of()).build(),
              battleActions);

      defensiveAaFire.execute(executionStack, delegateBridge);

      verify(battleActions, never()).fireDefensiveAaGuns();
    }
  }
}
