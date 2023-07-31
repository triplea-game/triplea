package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import java.util.List;
import org.triplea.java.ChangeOnNextMajorRelease;

@ChangeOnNextMajorRelease(
    "This should be reworked so that it doesn't inherit from CheckGeneralBattleEnd")
public class CheckStalemateBattleEnd extends CheckGeneralBattleEnd {
  public CheckStalemateBattleEnd(final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public List<StepDetails> getAllStepDetails() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.STALEMATE_BATTLE_END_CHECK;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (!getBattleState().getStatus().isOver() && isStalemate()) {
      getBattleActions().endBattle(IBattle.WhoWon.DRAW, bridge);
    }
  }
}
