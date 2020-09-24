package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;

/** Offensive Aa units can fire and the player can select their casualties */
public class DefensiveAaFire extends AaFireAndCasualtyStep {
  private static final long serialVersionUID = 3220057715007657960L;

  public DefensiveAaFire(final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public Order getOrder() {
    return Order.AA_DEFENSIVE;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (valid()) {
      battleActions.fireDefensiveAaGuns();
    }
  }

  @Override
  GamePlayer firingPlayer() {
    return battleState.getPlayer(DEFENSE);
  }

  @Override
  GamePlayer firedAtPlayer() {
    return battleState.getPlayer(OFFENSE);
  }

  @Override
  Collection<Unit> aaGuns() {
    return battleState.getAa(DEFENSE);
  }
}
