package games.strategy.triplea.delegate.battle.steps.change;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.CollectionUtils;

@AllArgsConstructor
public class MarkNoMovementLeft implements BattleStep {

  private static final long serialVersionUID = -7004369411317853693L;

  protected final BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<StepDetails> getAllStepDetails() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.MARK_NO_MOVEMENT_LEFT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (battleState.getStatus().isFirstRound() && !battleState.getStatus().isHeadless()) {
      final Collection<Unit> attackingNonAir =
          CollectionUtils.getMatches(
              battleState.filterUnits(ALIVE, OFFENSE), Matches.unitIsAir().negate());
      final Change noMovementChange = ChangeFactory.markNoMovementChange(attackingNonAir);
      if (!noMovementChange.isEmpty()) {
        bridge.addChange(noMovementChange);
      }
    }
  }
}
