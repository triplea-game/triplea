package games.strategy.triplea.delegate.battle.steps.fire.air;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.StepParameters;
import java.util.Collection;

/**
 * Air can not attack subs unless a destroyer is present
 *
 * <p>This step only occurs during naming so PRE_ROUND and IN_ROUND are the same
 */
public abstract class AirVsNonSubsStep extends BattleStep {

  public AirVsNonSubsStep(final StepParameters parameters) {
    super(parameters);
  }

  @Override
  public BattleAtomic getExecutable() {
    return null;
  }

  @Override
  protected void execute(final ExecutionStack stack, final IDelegateBridge bridge) {}

  protected boolean airWillMissSubs(
      final Collection<Unit> firingUnits, final Collection<Unit> firedAtUnits) {
    return firingUnits.stream().anyMatch(Matches.unitIsAir())
        && firingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && firedAtUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll());
  }
}
