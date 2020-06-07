package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public abstract class BattleStep {

  /** The current state of the battle */
  protected final BattleState battleState;

  /** Actions that can occur in a battle that require interaction with {@link IDelegateBridge} */
  protected final BattleActions battleActions;

  /** @return an atomic action in a battle */
  public abstract BattleAtomic getExecutable();

  /** @return a list of names that will be shown in {@link games.strategy.triplea.ui.BattlePanel} */
  public abstract List<String> getNames();

  /**
   * Determine if this step should run based on the request
   *
   * @return true if valid
   */
  public abstract boolean valid();

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
   *   <li>The code does not use IDelegateBridge
   *   <li>If the code uses IDelegateBridge, and an exception is called from one of those methods,
   *       the exception will be propagated out of execute() and the execute method can be called
   *       again.
   * </ol>
   */
  public abstract class BattleAtomic implements IExecutable {

    /**
     * Executes an atomic step in a battle
     *
     * @param stack The current stack of steps
     * @param bridge DelegateBridge for interacting with the rest of the program
     */
    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      final BattleStep executingStep = BattleStep.this;
      if (executingStep.valid()) {
        executingStep.execute(stack, bridge);
      }
    }
  }
}
