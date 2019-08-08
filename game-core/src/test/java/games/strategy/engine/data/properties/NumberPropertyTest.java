package games.strategy.engine.data.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class NumberPropertyTest {
  @Nested
  final class ConstructorTest {
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final int MAX_VALUE = 100;
    private static final int MIN_VALUE = 0;
    private static final int DEFAULT_VALUE = 42;

    @Test
    void shouldThrowExceptionWhenMaxValueLessThanMinValue() {
      final Exception e =
          assertThrows(
              IllegalArgumentException.class,
              () -> new NumberProperty(NAME, DESCRIPTION, MIN_VALUE - 1, MIN_VALUE, DEFAULT_VALUE));
      assertThat(e.getMessage(), is("Max must be greater than min"));
    }

    @Test
    void shouldThrowExceptionWhenDefaultValueGreaterThanMaxValue() {
      final Exception e =
          assertThrows(
              IllegalArgumentException.class,
              () -> new NumberProperty(NAME, DESCRIPTION, MAX_VALUE, MIN_VALUE, MAX_VALUE + 1));
      assertThat(e.getMessage(), is("Default value out of range"));
    }

    @Test
    void shouldThrowExceptionWhenDefaultValueLessThanMinValue() {
      final Exception e =
          assertThrows(
              IllegalArgumentException.class,
              () -> new NumberProperty(NAME, DESCRIPTION, MAX_VALUE, MIN_VALUE, MIN_VALUE - 1));
      assertThat(e.getMessage(), is("Default value out of range"));
    }
  }
}
