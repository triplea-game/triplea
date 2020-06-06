package games.strategy.triplea.delegate.battle.steps.fire.air;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_ATTACK_NON_SUBS;

import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.StepParameters;
import java.util.List;

/**
 * Air can not attack subs unless a destroyer is present
 *
 * <p>This step only occurs during naming so PRE_ROUND and IN_ROUND are the same
 */
public class AirAttackVsNonSubsStep extends AirVsNonSubsStep {
  public AirAttackVsNonSubsStep(final StepParameters parameters) {
    super(parameters);
  }

  @Override
  public List<String> getNames() {
    return List.of(AIR_ATTACK_NON_SUBS);
  }

  @Override
  public boolean valid(final BattleStep.Request request) {
    return airWillMissSubs(parameters.attackingUnits, parameters.defendingUnits);
  }
}
