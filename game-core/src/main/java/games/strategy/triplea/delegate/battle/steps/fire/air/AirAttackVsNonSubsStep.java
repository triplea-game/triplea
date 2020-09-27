package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_ATTACK_NON_SUBS;

import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;

/** Air can not attack subs unless a destroyer is present */
public class AirAttackVsNonSubsStep extends AirVsNonSubsStep {
  private static final long serialVersionUID = 4273449622231941896L;

  public AirAttackVsNonSubsStep(final BattleState battleState) {
    super(battleState);
  }

  @Override
  public List<String> getNames() {
    return valid() ? List.of(AIR_ATTACK_NON_SUBS) : List.of();
  }

  @Override
  public Order getOrder() {
    return Order.AIR_OFFENSIVE_NON_SUBS;
  }

  private boolean valid() {
    return airWillMissSubs(
        battleState.filterUnits(ALIVE, OFFENSE), battleState.filterUnits(ALIVE, DEFENSE));
  }
}
