package org.triplea.java.concurrency;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;

/**
 * Utility class to make the API of {@code CompletableFuture.runAsync} a bit nicer and avoids the
 * return null in the exception handler.
 *
 * <p>Example usage: <code>
 *   AsyncRunner.runAsync(() -> taskToRun())
 *      .exceptionally(throwable -> log.log(Level.Severe, "Error message", throwable));
 * </code>
 */
@UtilityClass
public class AsyncRunner {
  /** Runs a task using a default threadpool. */
  public static ExceptionHandler runAsync(final Runnable runnable) {
    return new ExceptionHandler(runnable, null);
  }

  /** Runs a task using the provided threadpool. */
  public static ExceptionHandler runAsync(final Runnable runnable, final Executor executor) {
    return new ExceptionHandler(runnable, executor);
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ExceptionHandler {
    private Runnable runnable;
    @Nullable private Executor executor;

    public void exceptionally(final Consumer<Throwable> exceptionHandler) {
      Optional.ofNullable(executor)
          .map(exec -> CompletableFuture.runAsync(runnable, exec))
          .orElseGet(() -> CompletableFuture.runAsync(runnable))
          .exceptionally(
              throwable -> {
                exceptionHandler.accept(throwable);
                return null;
              });
    }
  }
}
