package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.example.mockito.MockitoExtension;

import games.strategy.util.Interruptibles.InterruptibleRunnable;

@ExtendWith(MockitoExtension.class)
public final class InterruptiblesTest {
  @AfterEach
  public void resetTestThreadInterruptionStatusSoItDoesNotCrossTestBoundaries() {
    Thread.interrupted();
  }

  @Nested
  public final class AwaitTest {
    @Test
    public void shouldReturnFalseWhenNotInterrupted(@Mock final InterruptibleRunnable runnable) throws Exception {
      final boolean interrupted = Interruptibles.await(runnable);

      verify(runnable).run();
      assertThat(interrupted, is(false));
    }

    @Test
    public void shouldReturnTrueWhenInterrupted() {
      final boolean interrupted = Interruptibles.await(() -> {
        throw new InterruptedException();
      });

      assertThat(interrupted, is(true));
      assertThat(Thread.currentThread().isInterrupted(), is(true));
    }

    @Test
    public void shouldRethrowRunnableUncheckedException() {
      assertThrows(IllegalStateException.class, () -> Interruptibles.await(() -> {
        throw new IllegalStateException();
      }));
    }
  }

  @Nested
  public final class AwaitResultTest {
    @Test
    public void shouldReturnUninterruptedSupplierNonNullResultWhenNotInterrupted() {
      final Object value = new Object();

      final Interruptibles.Result<Object> result = Interruptibles.awaitResult(() -> value);

      assertThat(result.interrupted, is(false));
      assertThat(result.result, is(Optional.of(value)));
    }

    @Test
    public void shouldReturnUninterruptedSupplierNullResultWhenNotInterrupted() {
      final Interruptibles.Result<Object> result = Interruptibles.awaitResult(() -> null);

      assertThat(result.interrupted, is(false));
      assertThat(result.result, is(Optional.empty()));
    }

    @Test
    public void shouldReturnInterruptedEmptyResultWhenInterrupted() {
      final Interruptibles.Result<Object> result = Interruptibles.awaitResult(() -> {
        throw new InterruptedException();
      });

      assertThat(result.interrupted, is(true));
      assertThat(result.result, is(Optional.empty()));
      assertThat(Thread.currentThread().isInterrupted(), is(true));
    }

    @Test
    public void shouldRethrowSupplierUncheckedException() {
      assertThrows(IllegalStateException.class, () -> Interruptibles.awaitResult(() -> {
        throw new IllegalStateException();
      }));
    }
  }
}
