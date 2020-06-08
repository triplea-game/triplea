package games.strategy.triplea.delegate.battle.steps.fire.air;

import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import java.util.Collection;
import lombok.AllArgsConstructor;

/** Air can not attack subs unless a destroyer is present */
@AllArgsConstructor
public abstract class AirVsNonSubsStep implements BattleStep {

  /** The current state of the battle */
  protected final BattleState battleState;

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {}

  protected boolean airWillMissSubs(
      final Collection<Unit> firingUnits, final Collection<Unit> firedAtUnits) {
    return firingUnits.stream().anyMatch(Matches.unitIsAir())
        && firingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && firedAtUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll());
  }
}
