package games.strategy.util;

/**
 * A collection of methods useful for working with synchronous and asynchronous tasks.
 *
 * <p>
 * The methods of this class are thread safe.
 * </p>
 *
 * @author Brian Goetz
 */
public final class TaskUtil {
  private TaskUtil() {}

  /**
   * Launders the unknown cause of a task execution exception.
   *
   * <p>
   * Many of the Java concurrent programming framework task execution methods may throw an {@code ExecutionException}.
   * In such cases, the cause of the execution exception will fall into one of three categories: a checked exception
   * thrown by the task, a {@code RuntimeException}, or an {@code Error}. Developers are expected to test for known
   * checked exceptions and re-throw them. Unknown causes are then passed to this method to be properly handled.
   * </p>
   *
   * <p>
   * The following example shows the typical usage of this method:
   * </p>
   *
   * <pre>
   * try {
   *   return future.get(); // 'future' is of type {@literal java.util.concurrent.Future&lt;V&gt;}
   * } catch (ExecutionException e) {
   *   Throwable cause = e.getCause();
   *   if (cause instanceof MyCheckedException) {
   *     throw (MyCheckedException) cause;
   *   } else {
   *     throw TaskUtils.launderThrowable(cause);
   *   }
   * }
   * </pre>
   *
   * @param t The unknown cause of a task execution exception; may be {@code null}.
   *
   * @return The unchecked exception which caused the task execution exception.
   *
   * @throws Error If the cause was an error.
   * @throws IllegalStateException If the cause was a checked exception.
   */
  public static RuntimeException launderThrowable(final Throwable t) {
    if (t instanceof RuntimeException) {
      return (RuntimeException) t;
    } else if (t instanceof Error) {
      throw (Error) t;
    }

    throw new IllegalStateException("unexpected checked exception", t);
  }
}
