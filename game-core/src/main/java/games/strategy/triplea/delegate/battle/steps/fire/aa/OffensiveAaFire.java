package games.strategy.triplea.delegate.battle.steps.fire.aa;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;

/**
 * Offensive Aa units can fire and the player can select their casualties
 */
public class OffensiveAaFire extends AaFireAndCasualtyStep {
  private static final long serialVersionUID = 5843852442617511691L;

  public OffensiveAaFire(final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    if (valid()) {
      battleActions.fireOffensiveAaGuns();
    }
  }

  @Override
  GamePlayer firingPlayer() {
    return battleState.getAttacker();
  }

  @Override
  GamePlayer firedAtPlayer() {
    return battleState.getDefender();
  }

  @Override
  Collection<Unit> aaGuns() {
    return battleState.getOffensiveAa();
  }
}
