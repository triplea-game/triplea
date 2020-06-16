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
class OffensiveAaFireTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Nested
  class GetStep {
    @Test
    void aaOffensiveIfAaIsAvailable() {
      final BattleState battleState =
          givenBattleStateBuilder().offensiveAa(List.of(mock(Unit.class))).build();
      final OffensiveAaFire offensiveAaFire = new OffensiveAaFire(battleState, battleActions);
      assertThat(offensiveAaFire.getOrder(), is(Order.AA_OFFENSIVE));
    }

    @Test
    void skipIfNoAaIsAvailable() {
      final BattleState battleState = givenBattleStateBuilder().offensiveAa(List.of()).build();
      final OffensiveAaFire offensiveAaFire = new OffensiveAaFire(battleState, battleActions);
      assertThat(offensiveAaFire.getOrder(), is(Order.NOT_APPLICABLE));
    }
  }

  @Nested
  class FireAa {
    @Test
    void firedIfAaAreAvailable() {
      final OffensiveAaFire offensiveAaFire =
          new OffensiveAaFire(
              givenBattleStateBuilder().offensiveAa(List.of(mock(Unit.class))).build(),
              battleActions);

      offensiveAaFire.execute(executionStack, delegateBridge);

      verify(battleActions).fireOffensiveAaGuns();
    }

    @Test
    void notFiredIfNoAaAreAvailable() {
      final OffensiveAaFire offensiveAaFire =
          new OffensiveAaFire(
              givenBattleStateBuilder().offensiveAa(List.of()).build(), battleActions);

      offensiveAaFire.execute(executionStack, delegateBridge);

      verify(battleActions, never()).fireOffensiveAaGuns();
    }
  }
}
