package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_ATTACK_NON_SUBS;

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
  boolean valid(final Request request) {
    return airWillMissSubs(parameters.attackingUnits, parameters.defendingUnits);
  }
}
