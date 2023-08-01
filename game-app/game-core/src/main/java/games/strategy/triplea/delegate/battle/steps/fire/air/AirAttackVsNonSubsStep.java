package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.UnitBattleFilter.ALIVE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_ATTACK_NON_SUBS;

import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import org.triplea.java.RemoveOnNextMajorRelease;

/** Air can not attack subs unless a destroyer is present */
@RemoveOnNextMajorRelease
@Deprecated
public class AirAttackVsNonSubsStep extends AirVsNonSubsStep {
  private static final long serialVersionUID = 4273449622231941896L;

  public AirAttackVsNonSubsStep(final BattleState battleState) {
    super(battleState);
  }

  @Override
  public List<StepDetails> getAllStepDetails() {
    return valid() ? List.of(new StepDetails(AIR_ATTACK_NON_SUBS, this)) : List.of();
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
