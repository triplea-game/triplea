package games.strategy.engine.data.properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public final class GamePropertiesTest {
  @Test
  public void shouldBeAbleToRoundTripEditableProperties() throws Exception {
    final List<IEditableProperty> expected = Arrays.asList(
        new StringProperty("name1", "description1", "value1"),
        new StringProperty("name2", "description2", "value2"),
        new StringProperty("name3", "description3", "value3"));

    final byte[] bytes = GameProperties.writeEditableProperties(expected);
    final List<IEditableProperty> actual = GameProperties.readEditableProperties(bytes);

    assertThat(actual, is(expected));
  }
}
