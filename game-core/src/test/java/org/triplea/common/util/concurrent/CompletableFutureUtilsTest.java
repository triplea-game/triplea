package org.triplea.common.util.concurrent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.triplea.common.util.concurrent.CompletableFutureUtils.logExceptionWhenComplete;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class CompletableFutureUtilsTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class LogExceptionWhenCompleteTest {
    private static final String ERROR_MESSAGE = "error message";

    @Mock
    private Logger logger;

    @Test
    void shouldNotWriteLogWhenFutureCompletesNormally() {
      final CompletableFuture<Object> future = new CompletableFuture<>();
      logExceptionWhenComplete(future, ERROR_MESSAGE, logger);
      future.complete(new Object());

      verify(logger, never()).log(any(Level.class), anyString(), any(Throwable.class));
    }

    @Test
    void shouldWriteLogWhenFutureCompletesExceptionally() {
      final CompletableFuture<Object> future = new CompletableFuture<>();
      logExceptionWhenComplete(future, ERROR_MESSAGE, logger);
      final Exception ex = new Exception();
      future.completeExceptionally(ex);

      verify(logger).log(Level.SEVERE, ERROR_MESSAGE, ex);
    }
  }
}
