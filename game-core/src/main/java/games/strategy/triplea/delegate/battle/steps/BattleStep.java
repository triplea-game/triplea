package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.steps.BattleStep.State.IN_ROUND;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * A step in a battle.
 *
 * <p>#valid is called to determine if the step should be shown or executed.
 */
@AllArgsConstructor
public abstract class BattleStep {

  protected final StepParameters parameters;

  /**
   * BattleStep's conditions are checked before the round actually happens and then later during the
   * round.
   */
  public enum State {
    // Check if the step may occur during the round
    PRE_ROUND,
    // Check if the step can occur right now
    IN_ROUND,
  }

  public abstract IExecutable getExecutable();

  public abstract List<String> getNames();

  public abstract boolean valid(State state);

  protected abstract void execute(ExecutionStack stack, IDelegateBridge bridge);

  /**
   * A BattleExecutable is used to break up the battle into separate atomic pieces. If there is a
   * network error, or some other unfortunate event, then we need to keep track of what pieces we
   * have executed, and what is left to do. Each atomic step is in its own BattleExecutable with the
   * definition of atomic is that either:
   *
   * <ol>
   *   <li>The code does not call to an IDisplay, IPlayer, or IRandomSource
   *   <li>If the code calls to an IDisplay, IPlayer, IRandomSource, and an exception is called from
   *       one of those methods, the exception will be propagated out of execute() and the execute
   *       method can be called again.
   * </ol>
   */
  protected abstract static class BattleAtomic implements IExecutable {

    /**
     * Executes an atomic step in a battle
     *
     * @param stack The current stack of steps
     * @param bridge DelegateBridge for interacting with the rest of the program
     */
    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      final BattleStep executingStep = getStep();
      if (executingStep.valid(IN_ROUND)) {
        executingStep.execute(stack, bridge);
      }
    }

    protected abstract BattleStep getStep();
  }
}
