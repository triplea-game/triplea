package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RemoveNonCombatants implements BattleStep {

  private static final long serialVersionUID = 7629566123535773501L;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.REMOVE_NON_COMBATANTS;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    battleActions.removeNonCombatants(bridge);
  }
}
