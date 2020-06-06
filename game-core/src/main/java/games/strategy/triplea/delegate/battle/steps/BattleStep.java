package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.steps.BattleStep.Request.EXEC;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * A step in a battle.
 *
 * <ol>
 *   <ul>
 *     Each step can have 0 or more names. These names are shown in the {@link
 *     games.strategy.triplea.ui.BattlePanel}
 *   </ul>
 *   <ul>
 *     Each step can also have an executable. See {@link BattleAtomic} on what an executable is
 *     comprised of.
 *   </ul>
 * </ol>
 */
@AllArgsConstructor
public abstract class BattleStep {

  protected final StepParameters parameters;

  /** Indicates when {@link #valid} is being called */
  public enum Request {
    // Occurs at the start of the battle round
    NAME,
    // Occurs right before the step executes
    EXEC,
  }

  public abstract BattleAtomic getExecutable();

  public abstract List<String> getNames();

  /**
   * Determine if this step should run based on the request
   *
   * @param request Indicates when valid is being called
   * @return true if valid
   */
  public abstract boolean valid(Request request);

  /**
   * Executes the step
   *
   * <p>This is called by the BattleAtomic and {@link #valid} has already been checked
   *
   * @param stack The current stack of steps
   * @param bridge DelegateBridge for interacting with the rest of the program
   */
  protected abstract void execute(ExecutionStack stack, IDelegateBridge bridge);

  /**
   * This is used to break up the battle into separate atomic pieces. If there is a network error,
   * or some other unfortunate event, then we need to keep track of what pieces we have executed,
   * and what is left to do. Each atomic step is in its own BattleAtomic with the definition of
   * atomic is that either:
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
      if (executingStep.valid(EXEC)) {
        executingStep.execute(stack, bridge);
      }
    }

    /**
     * Get the BattleStep with the latest parameters
     *
     * @return an up-to-date BattleStep
     */
    protected abstract BattleStep getStep();
  }
}
