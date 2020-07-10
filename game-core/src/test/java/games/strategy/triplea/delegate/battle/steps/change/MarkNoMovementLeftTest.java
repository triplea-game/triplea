package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarkNoMovementLeftTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  void runsOnFirstRound() {
    final BattleState battleState = givenBattleStateBuilder().battleRound(1).build();
    final MarkNoMovementLeft markNoMovementLeft =
        new MarkNoMovementLeft(battleState, battleActions);

    markNoMovementLeft.execute(executionStack, delegateBridge);

    verify(battleActions).markNoMovementLeft(eq(delegateBridge));
  }

  @Test
  void doesNotRunOnSecondRound() {
    final BattleState battleState = givenBattleStateBuilder().battleRound(2).build();
    final MarkNoMovementLeft markNoMovementLeft =
        new MarkNoMovementLeft(battleState, battleActions);

    markNoMovementLeft.execute(executionStack, delegateBridge);

    verify(battleActions, never()).markNoMovementLeft(eq(delegateBridge));
  }
}
