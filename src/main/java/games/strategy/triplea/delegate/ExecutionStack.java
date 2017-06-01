package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Stack;

import games.strategy.engine.delegate.IDelegateBridge;

/**
 * Utilility for tracking a sequence of executables.
 *
 * <p>
 * It works like this,
 * We pop the top of the stack, store it in current, then execute it.
 * While exececuting the current element, the current element can push
 * more execution items onto the stack.
 * </p>
 *
 * <p>
 * After execution has finished, we pop the next item, and execute it, repeating till nothing is left to exectue.
 * </p>
 *
 * <p>
 * If an exception occurs during execution, we retain a reference to the current item. When we start executing again, we
 * first push current
 * onto the stack. In this way, an item may execute more than once. An IExecutable should be aware of this.
 * </p>
 */
public class ExecutionStack implements Serializable {
  private static final long serialVersionUID = -8675285470515074530L;
  private IExecutable m_current;
  private final Stack<IExecutable> m_stack = new Stack<>();

  void execute(final IDelegateBridge bridge) {
    // we were interrupted before, resume where we left off
    if (m_current != null) {
      m_stack.push(m_current);
    }
    while (!m_stack.isEmpty()) {
      m_current = m_stack.pop();
      m_current.execute(this, bridge);
    }
    m_current = null;
  }

  void push(final Collection<IExecutable> executables) {
    for (final IExecutable ex : executables) {
      push(ex);
    }
  }

  public void push(final IExecutable executable) {
    m_stack.push(executable);
  }

  boolean isExecuting() {
    return m_current != null;
  }

  public boolean isEmpty() {
    return m_stack.isEmpty();
  }
}
