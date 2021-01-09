package games.strategy.triplea.delegate.battle.steps.change;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * Removes non combatants before the AA phase
 *
 * <p>This doesn't remove AA units that can fire this round because this occurs before the AA phase
 * and they still need to fire. This is different from {@link RemoveNonCombatants}.
 */
@AllArgsConstructor
public class RemoveNonCombatantsBeforeAA implements BattleStep {

  private static final long serialVersionUID = 337655823124444148L;

  protected BattleState battleState;

  protected final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return List.of();
  }

  @Override
  public Order getOrder() {
    return Order.REMOVE_NON_COMBATANTS_INITIAL;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    removeNonCombatants(BattleState.Side.OFFENSE, bridge);
    removeNonCombatants(BattleState.Side.DEFENSE, bridge);
  }

  private void removeNonCombatants(final BattleState.Side side, final IDelegateBridge bridge) {
    final Collection<Unit> nonCombatants = battleState.removeNonCombatants(side, false);
    if (nonCombatants.isEmpty()) {
      return;
    }
    bridge
        .getDisplayChannelBroadcaster()
        .changedUnitsNotification(
            battleState.getBattleId(), battleState.getPlayer(side), nonCombatants, null, null);
  }
}
