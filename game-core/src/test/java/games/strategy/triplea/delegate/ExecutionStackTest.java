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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import games.strategy.engine.delegate.IDelegateBridge;

class ExecutionStackTest {
  private final ExecutionStack dummy = new ExecutionStack();

  @Test
  void testStackIsProcessedInRightOrder() {
    final List<Integer> orderCheck = new ArrayList<>();
    dummy.push(Arrays.asList(
        (stack, bridge) -> orderCheck.add(4),
        (stack, bridge) -> orderCheck.add(3)));
    dummy.push((stack, bridge) -> {
      // Prevent infinite loop
      if (orderCheck.contains(1)) {
        fail("Executable was executed more than once");
      }

      orderCheck.add(1);
      dummy.push((s, b) -> orderCheck.add(2));
    });
    dummy.push((stack, bridge) -> orderCheck.add(0));

    dummy.execute(null);

    assertThat(orderCheck.toArray(new Integer[0]), is(new Integer[]{0, 1, 2, 3, 4}));
  }

  @Test
  void testExecuteProcessesExecutable() {
    final IExecutable mock = mock(IExecutable.class);
    final IDelegateBridge bridge = mock(IDelegateBridge.class);

    dummy.push(mock);
    dummy.execute(bridge);

    verify(mock).execute(dummy, bridge);
  }

  @Test
  void testIsEmpty() {
    assertThat(dummy.isEmpty(), is(true));

    final IExecutable mock = mock(IExecutable.class);

    dummy.push(mock);
    assertThat(dummy.isEmpty(), is(false));
    dummy.execute(null);
    assertThat(dummy.isEmpty(), is(true));

    dummy.push(Arrays.asList(mock, mock));
    assertThat(dummy.isEmpty(), is(false));
    dummy.execute(null);
    assertThat(dummy.isEmpty(), is(true));
  }

  @Test
  void testExecutionStackIsAbortedCorrectly() {
    final IExecutable mock = mock(IExecutable.class);
    Mockito.doThrow(RuntimeException.class).doNothing().when(mock).execute(any(), any());
    dummy.push(mock);

    assertThrows(RuntimeException.class, () -> dummy.execute(null));

    assertDoesNotThrow(() -> dummy.execute(null));

    verify(mock, times(2)).execute(any(), any());
  }
}
