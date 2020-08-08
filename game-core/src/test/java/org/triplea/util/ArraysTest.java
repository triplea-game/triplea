package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.triplea.util.Arrays.withSensitiveArray;
import static org.triplea.util.Arrays.withSensitiveArrayAndReturn;

import java.util.function.Consumer;
import java.util.function.Function;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class ArraysTest {
  @Nested
  final class WithSensitiveArrayTest {
    private final char[] array = new char[] {'B', 'a', 'r'};

    @Test
    void shouldInvokeConsumer(@Mock final Consumer<char[]> consumer) {
      withSensitiveArray(() -> array, consumer);

      verify(consumer).accept(array);
    }

    @Test
    void shouldScrubArray() {
      withSensitiveArray(() -> array, it -> {});

      assertThat(array, is(new char[] {'\0', '\0', '\0'}));
    }
  }

  @Nested
  final class WithSensitiveArrayAndReturnTest {
    private final char[] array = new char[] {'B', 'a', 'r'};

    @Test
    void shouldReturnFunctionResult(@Mock final Function<char[], Integer> function) {
      when(function.apply(array)).thenReturn(42);

      final Integer result = withSensitiveArrayAndReturn(() -> array, function);

      assertThat(result, is(42));
      verify(function).apply(array);
    }

    @Test
    void shouldScrubArray() {
      withSensitiveArrayAndReturn(() -> array, Function.identity());

      assertThat(array, is(new char[] {'\0', '\0', '\0'}));
    }
  }
}
