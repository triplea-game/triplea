package games.strategy.triplea.delegate.battle.steps.change;

import static org.mockito.ArgumentMatchers.eq;
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
class RemoveNonCombatantsTest {

  @Mock ExecutionStack executionStack;
  @Mock IDelegateBridge delegateBridge;
  @Mock BattleState battleState;
  @Mock BattleActions battleActions;

  @Test
  void runs() {
    final RemoveNonCombatants removeNonCombatants =
        new RemoveNonCombatants(battleState, battleActions);

    removeNonCombatants.execute(executionStack, delegateBridge);

    verify(battleActions).removeNonCombatants(eq(delegateBridge));
  }
}
