package games.strategy.test;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A comparison function that determines if two objects are equal.
 *
 * <p>
 * For some types, the {@link Object#equals(Object)} method cannot be overridden, or it has been implemented incorrectly
 * and the implementation cannot be changed in order to preserve legacy behavior. In those cases, an equality comparator
 * can be defined for the type with the correct definition of what it means for two instances of that type to be equal.
 * </p>
 *
 * @see EqualityComparatorRegistry
 * @see TestUtil#equals(EqualityComparatorRegistry, Object, Object)
 */
@Immutable
public final class EqualityComparator {
  private final Predicate<Object> predicate;
  private final Class<?> type;

  private EqualityComparator(final Class<?> type, final Predicate<Object> predicate) {
    this.predicate = predicate;
    this.type = type;
  }

  /**
   * Indicates the specified objects are equal according to the definition of this comparator.
   *
   * @param context The comparator evaluation context.
   * @param o1 The first object to compare.
   * @param o2 The second object to compare.
   *
   * @return {@code true} if the specified objects are equal; otherwise {@code false}.
   */
  public boolean equals(final Context context, final Object o1, final Object o2) {
    checkNotNull(context);
    checkNotNull(o1);
    checkNotNull(o2);

    return predicate.test(context, o1, o2);
  }

  /**
   * Gets the type of object to compare by this comparator.
   *
   * @return The type of object to compare by this comparator.
   */
  public Class<?> getType() {
    return type;
  }

  /**
   * Creates a new equality comparator.
   *
   * @param type The type of object to compare.
   * @param predicate The predicate that tests two objects of the specified type for equality.
   *
   * @return A new new equality comparator.
   */
  public static <T> EqualityComparator newInstance(final Class<T> type, final Predicate<T> predicate) {
    checkNotNull(type);
    checkNotNull(predicate);

    final Predicate<Object> typeSafePredicate = (context, o1, o2) -> {
      try {
        return predicate.test(context, type.cast(o1), type.cast(o2));
      } catch (final ClassCastException e) {
        return false;
      }
    };
    return new EqualityComparator(type, typeSafePredicate);
  }

  /**
   * The evaluation context of an equality check.
   */
  public interface Context {
    /**
     * Indicates the specified objects are equal.
     *
     * <p>
     * {@link Predicate} implementors that need to perform further equality checks (e.g. when performing a deep equality
     * check), should delegate those checks to this method. For example, if a type has a child object that must be
     * included in the equality check, instead of using the expression
     * {@code this.childObject.equals(other.childObject)} or
     * {@code Objects.equals(this.childObject, other.childObject)}, one should instead use the expression
     * {@code context.equals(this.childObject, other.childObject)} to ensure any registered {@code EqualityComparator}
     * for the child object type is used.
     * </p>
     *
     * @param o1 The first object to compare.
     * @param o2 The second object to compare.
     *
     * @return {@code true} if the specified objects are equal; otherwise {@code false}.
     */
    boolean equals(@Nullable Object o1, @Nullable Object o2);
  }

  /**
   * Tests if two objects are equal in the context of an equality check based on {@code EqualityComparator}s.
   *
   * @param <T> The type of object to compare.
   */
  @FunctionalInterface
  public interface Predicate<T> {
    /**
     * Indicates the specified objects are equal.
     *
     * @param context The comparator evaluation context.
     * @param o1 The first object to compare.
     * @param o2 The second object to compare.
     *
     * @return {@code true} if the specified objects are equal; otherwise {@code false}.
     */
    boolean test(Context context, T o1, T o2);
  }
}
