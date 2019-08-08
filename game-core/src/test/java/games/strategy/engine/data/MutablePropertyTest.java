package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.data.MutableProperty.InvalidValueException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class MutablePropertyTest {
  @Nested
  final class SetValueTest {
    @Test
    void shouldThrowExceptionWhenValueHasWrongType() {
      final MutableProperty<Integer> mutableProperty =
          MutableProperty.ofSimple(value -> {}, () -> 42);

      final Exception e =
          assertThrows(InvalidValueException.class, () -> mutableProperty.setValue(new Object()));
      assertThat(e.getCause(), is(instanceOf(ClassCastException.class)));
    }
  }
}
