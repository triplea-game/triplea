package org.triplea.test.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.regex.Pattern;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

class CustomMatcherTest {
  /**
   * Example output using a matcher with a failure injected.:
   *
   * <pre>
   * assertThat(500, numberIsEqualToExampleMatcher(3));
   *
   * java.lang.AssertionError:
   * Expected: Test value should have been equal to: 3
   *      but: 500
   * </pre>
   */
  @Test
  void exampleWithDefaultBehavior() {
    assertThat(0, numberIsEqualToExampleMatcher(0));
    assertThat(5, numberIsEqualToExampleMatcher(5));
  }

  private static Matcher<Integer> numberIsEqualToExampleMatcher(final int expected) {
    return CustomMatcher.<Integer>builder()
        .description("Test value should have been equal to: " + expected)
        .checkCondition(testValue -> testValue == expected)
        .build();
  }

  @Test
  void basicMismatchCase() {
    assertThrows(AssertionError.class, () -> assertThat(0, numberIsEqualToExampleMatcher(100)));
  }

  /**
   * Example output using a matcher with a failure injected.:
   *
   * <pre>
   * assertThat(
   *     "Hashcode values are expected to match between 'abc' and 'abc'",
   *     "abc",
   *     hashCodesMatch("abc"));
   *
   * java.lang.AssertionError: In this example, if there is a failure,
   * the ..[shortened]... get the #toString of 'abc'
   * Expected: Expected hashcode: 2987023 (hashed from: abc1)
   *      but: Hashcode value is: 96354
   * </pre>
   */
  @Test
  void exampleWithTestValueDebug() {
    assertThat(
        "Hashcode values are expected to match between 'abc' and 'abc'",
        "abc",
        hashCodesMatch("abc"));

    final Throwable e =
        assertThrows(
            AssertionError.class,
            () ->
                assertThat(
                    "The hashcodes of different strings do not match, so we'll expect an "
                        + "exception here. This test can be used as an example to demo the debug "
                        + "messaging we get on a a failure by commenting out the assertThrows",
                    "123",
                    hashCodesMatch("abc")));
    assertThat(
        e.getMessage(),
        matchesPattern(line("Expected: Expected hash code: \\d+ \\(hashed from: abc\\)")));
    assertThat(e.getMessage(), matchesPattern(line("     but: Hash code value is: \\d+")));
  }

  private static Matcher<String> hashCodesMatch(final String value) {
    return CustomMatcher.<String>builder()
        .description("Expected hash code: " + value.hashCode() + " (hashed from: " + value + ")")
        .checkCondition(testValue -> testValue.hashCode() == value.hashCode())
        .debug(testValue -> "Hash code value is: " + testValue.hashCode())
        .build();
  }

  private static Pattern line(final String regex) {
    return Pattern.compile(".*^" + regex + "$.*", Pattern.DOTALL | Pattern.MULTILINE);
  }
}
