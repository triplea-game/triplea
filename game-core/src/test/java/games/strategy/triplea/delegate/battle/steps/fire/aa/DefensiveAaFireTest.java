package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep.Order;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefensiveAaFireTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Nested
  class GetOrder {
    @Test
    void aaDefensiveIfAaIsAvailable() {
      final BattleState battleState =
          givenBattleStateBuilder().defendingAa(List.of(mock(Unit.class))).build();
      final DefensiveAaFire defensiveAaFire = new DefensiveAaFire(battleState, battleActions);
      assertThat(defensiveAaFire.getOrder(), is(Order.AA_DEFENSIVE));
    }

    @Test
    void skipIfNoAaIsAvailable() {
      final BattleState battleState = givenBattleStateBuilder().defendingAa(List.of()).build();
      final DefensiveAaFire defensiveAaFire = new DefensiveAaFire(battleState, battleActions);
      assertThat(defensiveAaFire.getOrder(), is(Order.SKIP));
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
              givenBattleStateBuilder().defendingAa(List.of()).build(), battleActions);

      defensiveAaFire.execute(executionStack, delegateBridge);

      verify(battleActions, never()).fireDefensiveAaGuns();
    }
  }
}
