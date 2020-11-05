package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClearAaCasualtiesTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleActions battleActions;

  @Test
  void clearCasualtiesIfOffensiveAaExists() {
    final ClearAaCasualties clearAaCasualties =
        new ClearAaCasualties(givenBattleStateBuilder().build(), battleActions);

    clearAaCasualties.execute(executionStack, delegateBridge);

    verify(battleActions).clearWaitingToDieAndDamagedChangesInto(eq(delegateBridge));
  }
}
