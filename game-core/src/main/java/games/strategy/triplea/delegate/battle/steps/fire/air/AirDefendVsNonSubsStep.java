package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_DEFEND_NON_SUBS;

import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;

/**
 * Air can not attack subs unless a destroyer is present
 *
 * <p>This step only occurs during naming so PRE_ROUND and IN_ROUND are the same
 */
public class AirDefendVsNonSubsStep extends AirVsNonSubsStep {
  public AirDefendVsNonSubsStep(final BattleState battleState, final BattleActions battleActions) {
    super(battleState, battleActions);
  }

  @Override
  public List<String> getNames() {
    return List.of(AIR_DEFEND_NON_SUBS);
  }

  @Override
  public boolean valid(final Request request) {
    return airWillMissSubs(battleState.getDefendingUnits(), battleState.getAttackingUnits());
  }
}
