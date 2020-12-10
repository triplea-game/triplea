package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;

import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;

/** Offensive Aa units can fire and the player can select their casualties */
public class OffensiveAaFire extends AaFireAndCasualtyStep {
  private static final long serialVersionUID = 5843852442617511691L;

  private static final BattleState.Side side = OFFENSE;

  public OffensiveAaFire(final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public Order getOrder() {
    return Order.AA_OFFENSIVE;
  }

  @Override
  BattleState.Side getSide() {
    return side;
  }
}
