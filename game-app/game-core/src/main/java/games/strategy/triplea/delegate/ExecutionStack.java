package games.strategy.triplea.delegate;

import games.strategy.engine.delegate.IDelegateBridge;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/**
 * Utility for tracking a sequence of executables.
 *
 * <p>It works like this: We pop the top of the stack, store it in current, then execute it. While
 * executing the current element, the current element can push more execution items onto the stack.
 *
 * <p>After execution has finished, we pop the next item, and execute it, repeating till nothing is
 * left to execute.
 *
 * <p>If an exception occurs during execution, we retain a reference to the current item. When we
 * start executing again, we first push current onto the stack. In this way, an item may execute
 * more than once. An IExecutable should be aware of this.
 */
public class ExecutionStack implements Serializable {
  private static final long serialVersionUID = -8675285470515074530L;
  private IExecutable currentStep;
  private final Deque<IExecutable> deque = new ArrayDeque<>();

  public void execute(final IDelegateBridge bridge) {
    // we were interrupted before, resume where we left off
    if (currentStep != null) {
      currentStep.execute(this, bridge);
    }
    while (!deque.isEmpty()) {
      currentStep = deque.pop();
      currentStep.execute(this, bridge);
    }
    currentStep = null;
  }

  void push(final Collection<IExecutable> executables) {
    executables.forEach(deque::push);
  }

  public void push(final IExecutable executable) {
    deque.push(executable);
  }

  public boolean isExecuting() {
    return currentStep != null;
  }

  public boolean isEmpty() {
    return deque.isEmpty();
  }
}
