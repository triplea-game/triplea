package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ClearAaCasualties implements BattleStep {
  private static final long serialVersionUID = 995118832242624142L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return null;
  }

  @Override
  public Order getOrder() {
    return Order.AA_REMOVE_CASUALTIES;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (!battleState.getOffensiveAa().isEmpty() || !battleState.getDefendingAa().isEmpty()) {
      battleActions.clearWaitingToDieAndDamagedChangesInto(bridge);
    }
  }
}
