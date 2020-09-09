package games.strategy.engine.data.gameparser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.util.Tuple;

final class GameParserTest {
  @Nested
  final class DecapitalizeTest {
    @Test
    void shouldReturnValueWithFirstCharacterDecapitalized() {
      List.of(
              Tuple.of("", ""),
              Tuple.of("N", "n"),
              Tuple.of("name", "name"),
              Tuple.of("Name", "name"),
              Tuple.of("NAME", "nAME"))
          .forEach(
              t -> {
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
