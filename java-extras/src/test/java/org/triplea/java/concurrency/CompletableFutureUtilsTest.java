package org.triplea.java.concurrency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final String ERROR_MESSAGE = "error message";

    @Mock private Logger logger;

    @Test
    void shouldNotWriteLogWhenFutureCompletesNormally() {
      final CompletableFuture<Object> future = new CompletableFuture<>();
      CompletableFutureUtils.logExceptionWhenComplete(future, ERROR_MESSAGE, logger);
      future.complete(new Object());

      verify(logger, never()).log(any(Level.class), anyString(), any(Throwable.class));
    }

    @Test
    void shouldWriteLogWhenFutureCompletesExceptionally() {
      final CompletableFuture<Object> future = new CompletableFuture<>();
      CompletableFutureUtils.logExceptionWhenComplete(future, ERROR_MESSAGE, logger);
      final Exception ex = new Exception();
      future.completeExceptionally(ex);

      verify(logger).log(Level.SEVERE, ERROR_MESSAGE, ex);
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class ToCompletableFuture {

    @Mock private Future<Integer> throwingFuture;

    @Test
    void exceptionalCase() throws Exception {
      when(throwingFuture.get()).thenThrow(new InterruptedException());

      assertThrows(
          CompletionException.class,
          () -> CompletableFutureUtils.toCompletableFuture(throwingFuture).join());
    }

    @Test
    void nonExceptionalCase() throws Exception {
      final int value = 20;
      final Future<Integer> future = Executors.newFixedThreadPool(1).submit(() -> value);

      final int result = CompletableFutureUtils.toCompletableFuture(future).get();

      assertThat(result, is(value));
    }
  }
}
