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
  private static final long serialVersionUID = 4641526323094044712L;

  protected final BattleState battleState;

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    // Don't do anything because this step is only a visual indicator in the BattleUI
  }

  protected boolean airWillMissSubs(
      final Collection<Unit> firingUnits, final Collection<Unit> firedAtUnits) {
    return firingUnits.stream().anyMatch(Matches.unitIsAir())
        && firingUnits.stream().noneMatch(Matches.unitIsDestroyer())
        && firedAtUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll());
  }
}
