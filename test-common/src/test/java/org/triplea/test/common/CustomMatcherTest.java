package org.triplea.test.common;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class CustomMatcherTest {


  /**
   * Example output using a matcher with a failure injected.:
   *
   * <pre>
   *       MatcherAssert.assertThat(500, numberIsEqualToExampleMatcher(3));
   java.lang.AssertionError:
   Expected: Test value should have been equal to: 3
   but: 500
   * </pre>
   */
  @Test
  void exampleWithDefaultBehavior() {
    MatcherAssert.assertThat(0, numberIsEqualToExampleMatcher(0));
    MatcherAssert.assertThat(5, numberIsEqualToExampleMatcher(5));
  }

  private static Matcher<Integer> numberIsEqualToExampleMatcher(final int expected) {
    return CustomMatcher.<Integer>builder()
        .description("Test value should have been equal to: " + expected)
        .checkCondition(testValue -> testValue == expected)
        .build();
  }

  @Test
  void basicMismatchCase() {
    assertThrows(AssertionError.class, () -> MatcherAssert.assertThat(0, numberIsEqualToExampleMatcher(100)));
  }



  /**
   * Example output using a matcher with a failure injected.:
   *
   * <pre>
   *       MatcherAssert.assertThat(
   *         "Hashcode values are expected to match between 'abc' and 'abc'",
   *         "abc",
   *         hashCodesMatch("abc"));
   java.lang.AssertionError: In this example, if there is a failure, the ..[shortened]... get the #toString of 'abc'
   Expected: Expected hashcode: 2987023 (hashed from: abc1)
   but: Hashcode value is: 96354
   * </pre>
   */
  @Test
  public void exampleWithTestValueDebug() {
    MatcherAssert.assertThat(
        "Hashcode values are expected to match between 'abc' and 'abc'",
        "abc",
        hashCodesMatch("abc"));

    assertThrows(
        AssertionError.class,
        () -> MatcherAssert.assertThat(
            "The hashcodes of different strings do not match, so we'll expect an exception here."
                + "This test can be used as an example to demo the debug messaging we get on a a failure by "
                + "commenting out the assertThrows",
            "123",
            hashCodesMatch("abc")));
  }

  private static Matcher<String> hashCodesMatch(final String stringWithHashToMatch) {
    return CustomMatcher.<String>builder()
        .description(
            "Expected hashcode: " + stringWithHashToMatch.hashCode() + " ( hashed from: " + stringWithHashToMatch + ")")
        .checkCondition(testValue -> testValue.hashCode() == stringWithHashToMatch.hashCode())
        .debug(testValue -> "Hashcode value is: " + testValue.hashCode())
        .build();
  }
}
