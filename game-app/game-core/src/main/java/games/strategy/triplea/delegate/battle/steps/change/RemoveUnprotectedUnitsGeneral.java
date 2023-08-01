package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;

public class RemoveUnprotectedUnitsGeneral extends RemoveUnprotectedUnits {
  public RemoveUnprotectedUnitsGeneral(
      final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public List<StepDetails> getAllStepDetails() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.REMOVE_UNPROTECTED_UNITS_GENERAL;
  }
}
