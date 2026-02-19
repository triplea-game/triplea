package tools.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TestAttachment;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class MapPropertyWrapperTest {
  @Nested
  final class GetPropertyFieldTest {
    @Test
    void shouldReturnFieldWhenFieldExistsInTargetClass() {
      final Field field =
          MapPropertyWrapper.getPropertyField("doubleValue", ExampleChildAttachment.class);

      assertThat(field.getName(), is("doubleValue"));
      assertThat(field.getDeclaringClass(), is(ExampleChildAttachment.class));
    }

    @Test
    void shouldReturnFieldWhenFieldExistsInAncestorClass() {
      final Field field =
          MapPropertyWrapper.getPropertyField("intValue", ExampleChildAttachment.class);

      assertThat(field.getName(), is("intValue"));
      assertThat(field.getDeclaringClass(), is(ExampleParentAttachment.class));
    }

    @Test
    void shouldThrowExceptionWhenFieldDoesNotExist() {
      assertThrows(
          IllegalStateException.class,
          () -> MapPropertyWrapper.getPropertyField("xxx", TestAttachment.class));
    }
  }

  static class ExampleParentAttachment extends TestAttachment {
    private static final long serialVersionUID = 7376622767577501969L;

    int intValue;

    ExampleParentAttachment(
        final String name, final Attachable attachable, final GameData gameData) {
      super(name, attachable, gameData);
    }
  }

  static class ExampleChildAttachment extends ExampleParentAttachment {
    private static final long serialVersionUID = 8526929214887029959L;

    double doubleValue;

    ExampleChildAttachment(
        final String name, final Attachable attachable, final GameData gameData) {
      super(name, attachable, gameData);
    }
  }
}
