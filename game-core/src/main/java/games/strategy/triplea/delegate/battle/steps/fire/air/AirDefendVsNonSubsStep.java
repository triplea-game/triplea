package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_DEFEND_NON_SUBS;

import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;

/** Air can not attack subs unless a destroyer is present */
public class AirDefendVsNonSubsStep extends AirVsNonSubsStep {
  public AirDefendVsNonSubsStep(final BattleState battleState) {
    super(battleState);
  }

  @Override
  public List<String> getNames() {
    if (!valid()) {
      return List.of();
    }
    return List.of(AIR_DEFEND_NON_SUBS);
  }

  @Override
  public boolean valid() {
    return airWillMissSubs(battleState.getDefendingUnits(), battleState.getAttackingUnits());
  }
}
