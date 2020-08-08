package org.triplea.java.concurrency;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("InnerClassMayBeStatic")
final class CompletableFutureUtilsTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class LogExceptionWhenCompleteTest {
    @Mock private Consumer<Throwable> exceptionConsumer;

    @Test
    void shouldNotWriteLogWhenFutureCompletesNormally() {
      final CompletableFuture<Object> future = new CompletableFuture<>();
      CompletableFutureUtils.logExceptionWhenComplete(future, exceptionConsumer);
      future.complete(new Object());

      verify(exceptionConsumer, never()).accept(any(Throwable.class));
    }

    @Test
    void shouldWriteLogWhenFutureCompletesExceptionally() {
      final CompletableFuture<Object> future = new CompletableFuture<>();
      CompletableFutureUtils.logExceptionWhenComplete(future, exceptionConsumer);
      final Exception ex = new Exception();
      future.completeExceptionally(ex);

      verify(exceptionConsumer).accept(ex);
    }
  }
}
