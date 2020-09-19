package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.triplea.java.RemoveOnNextMajorRelease;

@RemoveOnNextMajorRelease(
    "CheckStalemateBattleEnd should be worked so that it doesn't inherit from CheckGeneralBattleEnd")
public class CheckStalemateBattleEnd extends CheckGeneralBattleEnd {
  public CheckStalemateBattleEnd(final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public List<String> getNames() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.STALEMATE_BATTLE_END_CHECK;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (isStalemate()) {
      getBattleActions().endBattle(bridge);
      getBattleActions().nobodyWins(bridge);
    }
  }
}
