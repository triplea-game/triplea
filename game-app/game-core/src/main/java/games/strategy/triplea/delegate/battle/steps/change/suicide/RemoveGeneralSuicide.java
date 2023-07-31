package games.strategy.triplea.delegate.battle.steps.change.suicide;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;

public class RemoveGeneralSuicide extends RemoveUnits {
  private static final long serialVersionUID = 9171979593312336763L;

  public RemoveGeneralSuicide(final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public List<StepDetails> getAllStepDetails() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.SUICIDE_REMOVE_CASUALTIES;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    removeUnits(bridge, Matches.unitIsFirstStrike().negate());
  }
}
