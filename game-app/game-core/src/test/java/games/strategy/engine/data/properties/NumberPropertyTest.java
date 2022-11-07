package games.strategy.engine.data.properties;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class NumberPropertyTest {
  private static final String NAME = "name";
  private static final String DESCRIPTION = "description";
  private static final int MAX_VALUE = 100;
  private static final int MIN_VALUE = 0;
  private static final int DEFAULT_VALUE = 42;

  @ParameterizedTest
  @MethodSource
  void shouldThrow(final Executable numberPropertySupplier) {
    assertThrows(IllegalArgumentException.class, numberPropertySupplier);
  }

  @SuppressWarnings("unused")
  static List<Executable> shouldThrow() {
    return List.of(
        () -> new NumberProperty(NAME, DESCRIPTION, MIN_VALUE - 1, MIN_VALUE, DEFAULT_VALUE),
        () -> new NumberProperty(NAME, DESCRIPTION, MAX_VALUE, MIN_VALUE, MAX_VALUE + 1),
        () -> new NumberProperty(NAME, DESCRIPTION, MAX_VALUE, MIN_VALUE, MIN_VALUE - 1));
  }
}
