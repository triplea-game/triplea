package tools.map.making;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataComponent;
import games.strategy.engine.xml.TestAttachment;

public final class MapPropertyWrapperTest {
  @Nested
  public final class GetPropertyFieldTest {
    @Test
    public void shouldReturnFieldWhenFieldWithEmUnderscorePrefixExistsInTargetClass() {
      final Field field = MapPropertyWrapper.getPropertyField("value", TestAttachment.class);

      assertThat(field.getName(), is("m_value"));
      assertThat(field.getDeclaringClass(), is(TestAttachment.class));
    }

    @Test
    public void shouldReturnFieldWhenFieldWithoutEmUnderscorePrefixExistsInTargetClass() {
      final Field field = MapPropertyWrapper.getPropertyField("doubleValue", ExampleChildAttachment.class);

      assertThat(field.getName(), is("doubleValue"));
      assertThat(field.getDeclaringClass(), is(ExampleChildAttachment.class));
    }

    @Test
    public void shouldReturnFieldWhenFieldWithEmUnderscorePrefixExistsInAncestorClass() {
      final Field field = MapPropertyWrapper.getPropertyField("data", TestAttachment.class);

      assertThat(field.getName(), is("m_data"));
      assertThat(field.getDeclaringClass(), is(GameDataComponent.class));
    }

    @Test
    public void shouldReturnFieldWhenFieldWithoutEmUnderscorePrefixExistsInAncestorClass() {
      final Field field = MapPropertyWrapper.getPropertyField("intValue", ExampleChildAttachment.class);

      assertThat(field.getName(), is("intValue"));
      assertThat(field.getDeclaringClass(), is(ExampleParentAttachment.class));
    }

    @Test
    public void shouldThrowExceptionWhenFieldDoesNotExist() {
      assertThrows(IllegalStateException.class, () -> MapPropertyWrapper.getPropertyField("xxx", TestAttachment.class));
    }
  }

  static class ExampleParentAttachment extends TestAttachment {
    private static final long serialVersionUID = 7376622767577501969L;

    int intValue;

    ExampleParentAttachment(final String name, final Attachable attachable, final GameData gameData) {
      super(name, attachable, gameData);
    }
  }

  static class ExampleChildAttachment extends ExampleParentAttachment {
    private static final long serialVersionUID = 8526929214887029959L;

    double doubleValue;

    ExampleChildAttachment(final String name, final Attachable attachable, final GameData gameData) {
      super(name, attachable, gameData);
    }
  }
}
