package games.strategy.test;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * A collection of matchers applicable for all types.
 */
public final class Matchers {
  private Matchers() {}

  /**
   * Creates a matcher that matches when the examined object is logically equal to the specified object.
   *
   * <p>
   * The returned matcher uses {@link TestUtil#equals(EqualityComparatorRegistry, Object, Object)} to compare the
   * specified objects for equality. To customize how the equality comparisons are performed, pass an equality
   * comparator registry with the required equality comparators to
   * {@link IsEqual#withEqualityComparatorRegistry(EqualityComparatorRegistry)}. Any type that does not have a
   * registered equality comparator will fall back to using {@link Object#equals(Object)}.
   * </p>
   *
   * @param expected The expected value.
   *
   * @return A new matcher.
   */
  public static <T> IsEqual<T> equalTo(final @Nullable T expected) {
    return new IsEqual<>(expected);
  }

  /**
   * A matcher that matches two objects for equality using
   * {@link TestUtil#equals(EqualityComparatorRegistry, Object, Object)}.
   *
   * @param <T> The type of object to compare for equality.
   */
  public static final class IsEqual<T> extends BaseMatcher<T> {
    private EqualityComparatorRegistry equalityComparatorRegistry;
    private final @Nullable Object expected;

    private IsEqual(final @Nullable Object expected) {
      this.expected = expected;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendValue(expected);
    }

    @Override
    public boolean matches(final @Nullable Object actual) {
      return TestUtil.equals(getEqualityComparatorRegistry(), expected, actual);
    }

    private EqualityComparatorRegistry getEqualityComparatorRegistry() {
      return (equalityComparatorRegistry != null)
          ? equalityComparatorRegistry
          : EqualityComparatorRegistry.newInstance();
    }

    /**
     * Sets the equality comparator registry to use during the equality comparison.
     *
     * @param equalityComparatorRegistry The equality comparator registry to use.
     *
     * @return A reference to this matcher.
     */
    public IsEqual<T> withEqualityComparatorRegistry(final EqualityComparatorRegistry equalityComparatorRegistry) {
      checkNotNull(equalityComparatorRegistry);

      this.equalityComparatorRegistry = equalityComparatorRegistry;
      return this;
    }
  }
}
