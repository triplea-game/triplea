package games.strategy.triplea.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import games.strategy.engine.delegate.IDelegateBridge;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ExecutionStackTest {
  private final ExecutionStack executionStack = new ExecutionStack();

  @Test
  void testStackIsProcessedInRightOrder() {
    final List<Integer> orderCheck = new ArrayList<>();
    executionStack.push(
        List.of((stack, bridge) -> orderCheck.add(4), (stack, bridge) -> orderCheck.add(3)));
    executionStack.push(
        (stack, bridge) -> {
          // Prevent infinite loop
          if (orderCheck.contains(1)) {
            fail("Executable was executed more than once");
          }

          orderCheck.add(1);
          executionStack.push((s, b) -> orderCheck.add(2));
        });
    executionStack.push((stack, bridge) -> orderCheck.add(0));

    executionStack.execute(null);

    assertThat(orderCheck.toArray(new Integer[0]), is(new Integer[] {0, 1, 2, 3, 4}));
  }

  @Test
  void testExecuteProcessesExecutable() {
    final IExecutable mock = mock(IExecutable.class);
    final IDelegateBridge bridge = mock(IDelegateBridge.class);

    executionStack.push(mock);
    executionStack.execute(bridge);

    verify(mock).execute(executionStack, bridge);
  }

  @Test
  void testIsEmpty() {
    assertThat(executionStack.isEmpty(), is(true));

    final IExecutable mock = mock(IExecutable.class);

    executionStack.push(mock);
    assertThat(executionStack.isEmpty(), is(false));
    executionStack.execute(null);
    assertThat(executionStack.isEmpty(), is(true));

    executionStack.push(List.of(mock, mock));
    assertThat(executionStack.isEmpty(), is(false));
    executionStack.execute(null);
    assertThat(executionStack.isEmpty(), is(true));
  }

  @Test
  void testExecutionStackIsAbortedCorrectly() {
    final IExecutable mock = mock(IExecutable.class);
    Mockito.doThrow(RuntimeException.class).doNothing().when(mock).execute(any(), any());
    executionStack.push(mock);

    assertThrows(RuntimeException.class, () -> executionStack.execute(null));

    assertDoesNotThrow(() -> executionStack.execute(null));

    verify(mock, times(2)).execute(any(), any());
  }
}
