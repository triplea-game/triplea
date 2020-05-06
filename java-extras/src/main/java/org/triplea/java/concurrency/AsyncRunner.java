package org.triplea.java.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * Utility class to make the API of CompletableFuture.runAsync a bit nicer and avoids the return
 * null in the exception handler.
 *
 * <p>Example usage: <code>
 *   AsyncRunner.runAsync(() -> taskToRun())
 *      .exceptionally(throwable -> log.log(Level.Severe, "Error message", throwable));
 * </code>
 */
@UtilityClass
public class AsyncRunner {
  public static ExceptionHandler runAsync(final Runnable runnable) {
    return new ExceptionHandler(runnable);
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ExceptionHandler {
    private Runnable runnable;

    public void exceptionally(final Consumer<Throwable> exceptionHandler) {
      CompletableFuture.runAsync(runnable)
          .exceptionally(
              throwable -> {
                exceptionHandler.accept(throwable);
                return null;
              });
    }
  }
}
