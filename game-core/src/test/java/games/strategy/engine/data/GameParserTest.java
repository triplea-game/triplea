package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.util.Tuple;

public final class GameParserTest {
  @Nested
  public final class DecapitalizeTest {
    @Test
    public void shouldReturnValueWithFirstCharacterDecapitalized() {
      Arrays.asList(
          Tuple.of("", ""),
          Tuple.of("N", "n"),
          Tuple.of("name", "name"),
          Tuple.of("Name", "name"),
          Tuple.of("NAME", "nAME"))
          .forEach(t -> {
            final String value = t.getFirst();
            final String decapitalizedValue = t.getSecond();
            assertThat(
                String.format("wrong decapitalization for '%s'", value),
                GameParser.decapitalize(value),
                is(decapitalizedValue));
          });
    }
  }
}
