package org.triplea.test.common;

import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Convenience class for building a type safe hamcrest matcher. See Test cases for more example
 * usages. Example usage:
 *
 * <pre>{@code
 * assertThat(testValue, customMatcher("valueToMatch"));
 *
 * private static Matcher<String> customMatcher(final String expectedValue) {
 *   return MatchBuilder.<String>builder()
 *       .description("We expect the test value to be equal to: " + expectedValue)
 *       .checkCondition(String::equals)
 *       .debug(testValue -> "This can be used to print more debug output when there is a failure"
 *          + ", it is optional, when omitted you will get a 'testValue.toString()' by default")
 *       .build();
 * }
 * }</pre>
 *
 * @param <T> The result type which we will be checking. For example, if we are testing a 'HashMap'
 *     has a specific value, then this would be of type 'HashMap'
 */
@Builder
public final class CustomMatcher<T> extends TypeSafeMatcher<T> {
  @Nonnull private final String description;
  @Nonnull private final Predicate<T> checkCondition;
  @Builder.Default @Nonnull private final Function<T, String> debug = Object::toString;

  @Override
  protected boolean matchesSafely(final T item) {
    return checkCondition.test(item);
  }

  @Override
  protected void describeMismatchSafely(final T item, final Description mismatchDescription) {
    mismatchDescription.appendText(debug.apply(item));
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText(this.description);
  }
}
