package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.List;
import org.triplea.java.ChangeOnNextMajorRelease;
import org.triplea.java.RemoveOnNextMajorRelease;

public class RemoveNonCombatants implements BattleStep {

  private static final long serialVersionUID = 7629566123535773501L;

  protected final BattleActions battleActions;

  @ChangeOnNextMajorRelease("Change order of members so that @AllArgsConstructor can be used")
  protected BattleState battleState;

  public RemoveNonCombatants(final BattleState battleState, final BattleActions battleActions) {
    this.battleState = battleState;
    this.battleActions = battleActions;
  }

  @Override
  public List<StepDetails> getAllStepDetails() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.REMOVE_NON_COMBATANTS;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    removeNonCombatants(BattleState.Side.OFFENSE, bridge);
    removeNonCombatants(BattleState.Side.DEFENSE, bridge);
  }

  private void removeNonCombatants(final BattleState.Side side, final IDelegateBridge bridge) {
    final Collection<Unit> nonCombatants = battleState.removeNonCombatants(side);
    if (nonCombatants.isEmpty()) {
      return;
    }
    bridge
        .getDisplayChannelBroadcaster()
        .changedUnitsNotification(
            battleState.getBattleId(), battleState.getPlayer(side), nonCombatants, null, null);
  }

  @RemoveOnNextMajorRelease("battleState will not need to be converted from battleActions")
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();

    if (battleState == null && battleActions instanceof BattleState) {
      // this works because the only BattleActions implementor also implements BattleState
      battleState = (BattleState) battleActions;
    }
  }
}
