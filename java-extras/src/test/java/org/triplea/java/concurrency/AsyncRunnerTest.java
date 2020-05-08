package org.triplea.java.concurrency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncRunnerTest {

  @Mock private Runnable runnable;
  @Mock private Consumer<Throwable> exceptionHandler;
  private final ArgumentCaptor<Throwable> exceptionArgumentCaptor =
      ArgumentCaptor.forClass(Throwable.class);

  @Test
  void runAsync() {
    AsyncRunner.runAsync(runnable) //
        .exceptionally(exceptionHandler);

    verify(runnable, timeout(1000)).run();
  }

  @Test
  void runAsyncWithCustomThreadPool() {
    AsyncRunner.runAsync(runnable, Executors.newFixedThreadPool(1)) //
        .exceptionally(exceptionHandler);

    verify(runnable, timeout(1000)).run();
  }

  @Test
  void callsExceptionOnError() {
    final RuntimeException exception = new RuntimeException("test");
    AsyncRunner.runAsync(
            () -> {
              throw exception;
            })
        .exceptionally(exceptionHandler);

    verify(exceptionHandler, timeout(1000)).accept(exceptionArgumentCaptor.capture());

    assertThat(
        "Throwable should be a completion exception",
        exceptionArgumentCaptor.getValue(),
        instanceOf(CompletionException.class));
    assertThat(
        "The cause of the completion exception should be the exception thrown by the runnable",
        exceptionArgumentCaptor.getValue().getCause(),
        is(exception));
  }
}
