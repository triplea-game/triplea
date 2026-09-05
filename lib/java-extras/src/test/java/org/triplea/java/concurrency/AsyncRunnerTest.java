package org.triplea.java.concurrency;

import static org.assertj.core.api.Assertions.assertThat;
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

    assertThat(exceptionArgumentCaptor.getValue())
        .as("Throwable should be a completion exception")
        .isInstanceOf(CompletionException.class);
    assertThat(exceptionArgumentCaptor.getValue().getCause())
        .as("The cause of the completion exception should be the exception thrown by the runnable")
        .isEqualTo(exception);
  }
}
