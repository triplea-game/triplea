package games.strategy.engine.data.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class GamePropertiesTest {
  @Nested
  final class ReadWriteEditablePropertiesTest {
    @Test
    void shouldBeAbleToRoundTripEditableProperties() throws Exception {
      final List<StringProperty> expected =
          List.of(
              new StringProperty("name1", "description1", "value1"),
              new StringProperty("name2", "description2", "value2"),
              new StringProperty("name3", "description3", "value3"));

      final byte[] bytes = GameProperties.writeEditableProperties(expected);
      final List<IEditableProperty<?>> actual = GameProperties.readEditableProperties(bytes);

      assertThat(actual, is(expected));
    }
  }
}
