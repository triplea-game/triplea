package org.triplea.test.common;

import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import lombok.Builder;

/**
 * Convenience class for building a type safe hamcrest matcher.
 * See Test cases for more example usages.
 * Example usage:
 *
 * <pre>
 * {@code
 *  assertThat(testValue, customMatcher("valueToMatch"));
 *
 *  private static Matcher<String> customMatcher(final String expectedValue) {
 *    return MatchBuilder.<String>builder()
 *        .description("We expect the test value to be equal to: " + expectedValue)
 *        .checkCondition(String::equals)
 *        .debug(testValue -> "This can be used to print more debug output when there is a failure"
 *           + ", it is optional, when omitted you will get a 'testValue.toString()' by default")
 *        .build();
 *  }
 * }
 * </pre>
 *
 * @param <T> The result type which we will be checking. For example, if we are testing a 'HashMap' has a specific
 *        value, then this would be of type 'HashMap'
 */
@Builder
public class CustomMatcher<T> extends BaseMatcher<T> {
  @Nonnull
  private final String description;
  @Nonnull
  private final Predicate<T> checkCondition;
  @SuppressWarnings("FieldMayBeFinal")
  @Builder.Default
  private Function<T, String> debug = Object::toString;

  @SuppressWarnings("unchecked")
  @Override
  public boolean matches(final Object item) {
    return checkCondition.test((T) item);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void describeMismatch(final Object item, final Description description) {
    description.appendText(debug.apply((T) item));
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText(this.description);
  }
}
