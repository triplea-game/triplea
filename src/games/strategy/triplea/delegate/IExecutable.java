package games.strategy.triplea.delegate;

import java.io.Serializable;

import games.strategy.engine.delegate.IDelegateBridge;

public interface IExecutable extends Serializable {
  /**
   * See the documentation to IExecutionStack.
   */
  public void execute(ExecutionStack stack, IDelegateBridge bridge);
}
