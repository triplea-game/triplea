package games.strategy.triplea.delegate;

import games.strategy.engine.delegate.IDelegateBridge;
import java.io.Serializable;

/**
 * A persistable action executed by a delegate.
 *
 * @see ExecutionStack
 */
public interface IExecutable extends Serializable {
  /** See the documentation to IExecutionStack. */
  void execute(ExecutionStack stack, IDelegateBridge bridge);
}
