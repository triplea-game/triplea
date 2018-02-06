package games.strategy.engine.delegate;

import static games.strategy.test.Assertions.assertNotThrows;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public final class DelegateExecutionManagerTest {
  private final DelegateExecutionManager delegateExecutionManager = new DelegateExecutionManager();

  @Test
  public void shouldAllowSerialExecutionOfDelegatesOnSameThread() {
    // given: a delegate is executed on the current thread and runs to completion
    delegateExecutionManager.enterDelegateExecution();
    delegateExecutionManager.leaveDelegateExecution();

    // when: a second delegate is executed on the current thread after the first delegate's execution ends
    // then: no exception should be thrown to prevent the second delegate's execution
    assertNotThrows(delegateExecutionManager::enterDelegateExecution);
  }

  @Test
  public void shouldNotAllowNestedExecutionOfDelegatesOnSameThread() {
    // given: a delegate is executed on the current thread
    delegateExecutionManager.enterDelegateExecution();

    // when: a second delegate is executed on the current thread while the first delegate is still running
    // then: an exception should be thrown to prevent the second delegate's execution
    assertThrows(IllegalStateException.class, delegateExecutionManager::enterDelegateExecution);
  }
}
