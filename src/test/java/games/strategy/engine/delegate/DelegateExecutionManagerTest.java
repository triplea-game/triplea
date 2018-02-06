package games.strategy.engine.delegate;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public final class DelegateExecutionManagerTest {
  private final DelegateExecutionManager delegateExecutionManager = new DelegateExecutionManager();

  @Test
  public void enterDelegateExecution_ShouldThrowExceptionWhenDelegateExecutingOnCurrentThread() {
    delegateExecutionManager.enterDelegateExecution();
    try {
      assertThrows(IllegalStateException.class, delegateExecutionManager::enterDelegateExecution);
    } finally {
      delegateExecutionManager.leaveDelegateExecution();
    }
  }
}
