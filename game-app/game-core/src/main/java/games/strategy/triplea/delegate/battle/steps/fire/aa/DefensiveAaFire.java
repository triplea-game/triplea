package games.strategy.triplea.delegate.battle.steps.fire.aa;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;

import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;

/** Offensive Aa units can fire and the player can select their casualties */
public class DefensiveAaFire extends AaFireAndCasualtyStep {
  private static final long serialVersionUID = 3220057715007657960L;

  private static final BattleState.Side side = DEFENSE;

  public DefensiveAaFire(final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public Order getOrder() {
    return Order.AA_DEFENSIVE;
  }

  @Override
  BattleState.Side getSide() {
    return side;
  }
}
