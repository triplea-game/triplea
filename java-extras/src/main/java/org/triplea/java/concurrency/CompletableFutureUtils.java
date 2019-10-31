package org.triplea.java.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;

/** A collection of useful methods for working with instances of {@link CompletableFuture}. */
@UtilityClass
public final class CompletableFutureUtils {

  /**
   * Invokes {@param exceptionHandler} with any exception thrown by {@code future} when it is
   * complete. If {@code future} completes normally, no action is taken.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static void logExceptionWhenComplete(
      final CompletableFuture<?> future, final Consumer<Throwable> exceptionHandler) {
    future.whenComplete(
        (result, ex) -> {
          if (ex != null) {
            exceptionHandler.accept(ex);
          }
        });
  }
}
