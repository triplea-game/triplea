package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_CASUALTIES;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ClearGeneralCasualties implements BattleStep {
  private static final long serialVersionUID = 995118832242624142L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return List.of(new StepDetails(REMOVE_CASUALTIES, this));
  }

  @Override
  public Order getOrder() {
    return Order.GENERAL_REMOVE_CASUALTIES;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    battleActions.clearWaitingToDieAndDamagedChangesInto(
        bridge, BattleState.Side.OFFENSE, BattleState.Side.DEFENSE);
  }
}
