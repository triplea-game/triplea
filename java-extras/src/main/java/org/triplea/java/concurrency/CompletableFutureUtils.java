package org.triplea.java.concurrency;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.extern.java.Log;

/** A collection of useful methods for working with instances of {@link CompletableFuture}. */
@Log
public final class CompletableFutureUtils {
  private CompletableFutureUtils() {}

  /**
   * Logs any exception thrown by {@code future} when it is complete. If {@code future} completes
   * normally, no action is taken.
   */
  public static void logExceptionWhenComplete(
      final CompletableFuture<?> future, final String message) {
    checkNotNull(future);
    checkNotNull(message);

    logExceptionWhenComplete(future, message, log);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  @VisibleForTesting
  static void logExceptionWhenComplete(
      final CompletableFuture<?> future, final String message, final Logger logger) {
    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            logger.log(Level.SEVERE, message, ex);
          }
        });
  }
}
