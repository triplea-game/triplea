package games.strategy.engine.random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class InternalDiceServerTest {

  @Nested
  final class GetDiceTest {
    private final InternalDiceServer internalDiceServer = new InternalDiceServer();

    private Integer[] getDice(final String string) {
      final int ignoredCount = -1;
      return Arrays.stream(internalDiceServer.getDice(string, ignoredCount)).boxed().toArray(Integer[]::new);
    }

    @Test
    void shouldReturnDiceWhenStringIsWellFormed() {
      assertThat(getDice("1"), is(arrayContaining(1)));
      assertThat(getDice("1,2"), is(arrayContaining(1, 2)));
      assertThat(getDice("1,2,3"), is(arrayContaining(1, 2, 3)));
    }

    @Test
    void shouldThrowExceptionWhenStringIsMalformed() {
      assertThrows(NumberFormatException.class, () -> getDice(""));
      assertThrows(NumberFormatException.class, () -> getDice("A"));
      assertThrows(NumberFormatException.class, () -> getDice("1,"));
      assertThrows(NumberFormatException.class, () -> getDice(",2"));
      assertThrows(NumberFormatException.class, () -> getDice("1,,3"));
      assertThrows(NumberFormatException.class, () -> getDice("1,A,3"));
    }
  }
}
