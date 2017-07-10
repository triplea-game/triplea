package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A collection of useful methods for working with instances of {@link Optional}.
 */
public final class OptionalUtils {
  private OptionalUtils() {}

  /**
   * If a value is present in the specified {@code Optional}, performs the given action with the value, otherwise
   * performs the given empty-based action.
   *
   * @param optional The {@code Optional} on which to operate; must not be {@code null}.
   * @param presentAction The action to be performed, if a value is present; must not be {@code null}.
   * @param emptyAction The empty-based action to be performed, if no value is present; must not be {@code null}.
   */
  public static <T> void ifPresentOrElse(
      final Optional<T> optional,
      final Consumer<? super T> presentAction,
      final Runnable emptyAction) {
    checkNotNull(optional);
    checkNotNull(presentAction);
    checkNotNull(emptyAction);

    if (optional.isPresent()) {
      presentAction.accept(optional.get());
    } else {
      emptyAction.run();
    }
  }
}
