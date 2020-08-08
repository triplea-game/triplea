package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_DEFEND_NON_SUBS;

import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;

/** Air can not attack subs unless a destroyer is present */
public class AirDefendVsNonSubsStep extends AirVsNonSubsStep {
  private static final long serialVersionUID = -7965786276905309057L;

  public AirDefendVsNonSubsStep(final BattleState battleState) {
    super(battleState);
  }

  @Override
  public List<String> getNames() {
    return valid() ? List.of(AIR_DEFEND_NON_SUBS) : List.of();
  }

  @Override
  public Order getOrder() {
    return Order.AIR_DEFENSIVE_NON_SUBS;
  }

  private boolean valid() {
    return airWillMissSubs(battleState.getDefendingUnits(), battleState.getAttackingUnits());
  }
}
