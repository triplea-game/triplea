package org.triplea.common.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.common.util.Arrays.withSensitiveArray;

import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class ArraysTest {
  @Nested
  final class WithSensitiveArrayTest {
    @Test
    void shouldReturnFunctionResult() {
      assertThat(withSensitiveArray(() -> new char[0], it -> 42), is(42));
    }

    @Test
    void shouldScrubArray() {
      final char[] array = new char[] {'B', 'a', 'r'};

      withSensitiveArray(() -> array, Function.identity());

      assertThat(array, is(new char[] {'\0', '\0', '\0'}));
    }
  }
}
