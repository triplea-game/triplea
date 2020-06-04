package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_ATTACK_NON_SUBS;

import java.util.ArrayList;
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
  public List<String> getStepNames() {
    final List<String> steps = new ArrayList<>();
    if (airWillMissSubs(parameters.attackingUnits, parameters.defendingUnits)) {
      steps.add(AIR_ATTACK_NON_SUBS);
    }
    return steps;
  }

  @Override
  public boolean valid(final State state) {
    return airWillMissSubs(parameters.attackingUnits, parameters.defendingUnits);
  }
}
