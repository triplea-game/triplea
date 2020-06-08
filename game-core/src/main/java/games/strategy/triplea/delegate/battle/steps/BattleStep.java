package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.List;
import lombok.AllArgsConstructor;

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
public interface BattleStep extends IExecutable {

  /** @return a list of names that will be shown in {@link games.strategy.triplea.ui.BattlePanel} */
  List<String> getNames();

  /**
   * Determine if this step should run based on the request
   *
   * @return true if valid
   */
  boolean valid();
}
