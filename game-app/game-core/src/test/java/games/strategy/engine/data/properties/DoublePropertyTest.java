package games.strategy.engine.data.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class DoublePropertyTest {
  @Nested
  final class ConstructorTest {
    @NonNls private static final String NAME = "name";
    @NonNls private static final String DESCRIPTION = "description";
    private static final double MAX_VALUE = 100.0;
    private static final double MIN_VALUE = 0.0;
    private static final double DEFAULT_VALUE = 42.0;
    private static final int NUMBER_OF_PLACES = 10;

    @Test
    void shouldThrowExceptionWhenMaxValueLessThanMinValue() {
      final Exception e =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new DoubleProperty(
                      NAME,
                      DESCRIPTION,
                      MIN_VALUE - 1.0,
                      MIN_VALUE,
                      DEFAULT_VALUE,
                      NUMBER_OF_PLACES));
      assertThat(e.getMessage(), is("Max must be greater than min"));
    }

    @Test
    void shouldThrowExceptionWhenDefaultValueGreaterThanMaxValue() {
      final Exception e =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new DoubleProperty(
                      NAME, DESCRIPTION, MAX_VALUE, MIN_VALUE, MAX_VALUE + 1.0, NUMBER_OF_PLACES));
      assertThat(e.getMessage(), is("Default value out of range"));
    }

    @Test
    void shouldThrowExceptionWhenDefaultValueLessThanMinValue() {
      final Exception e =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new DoubleProperty(
                      NAME, DESCRIPTION, MAX_VALUE, MIN_VALUE, MIN_VALUE - 1.0, NUMBER_OF_PLACES));
      assertThat(e.getMessage(), is("Default value out of range"));
    }
  }
}
