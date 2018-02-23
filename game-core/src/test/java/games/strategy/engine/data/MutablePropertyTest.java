package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.MutableProperty.InvalidValueException;

public final class MutablePropertyTest {
  @Test
  public void setValue_ShouldThrowExceptionWhenValueHasWrongType() {
    final MutableProperty<Integer> mutableProperty = MutableProperty.ofSimple(Integer.class, value -> {
    }, () -> 42);

    final Exception e = assertThrows(InvalidValueException.class, () -> mutableProperty.setValue(new Object()));
    assertThat(e.getCause(), is(instanceOf(ClassCastException.class)));
  }
}
