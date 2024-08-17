package games.strategy.engine.random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;

final class PlainRandomSourceTest {
  @NonNls private static final String ANNOTATION = "annotation";
  private static final int MAX = 6;

  private final PlainRandomSource plainRandomSource = new PlainRandomSource();

  private static void assertValueBetweenZeroInclusiveAndMaxExclusive(final int value) {
    assertThat(value, allOf(greaterThanOrEqualTo(0), lessThan(MAX)));
  }

  @Test
  void getRandomSingle_ShouldReturnValueBetweenZeroInclusiveAndMaxExclusive() {
    IntStream.range(0, 5_000)
        .forEach(
            i ->
                assertValueBetweenZeroInclusiveAndMaxExclusive(
                    plainRandomSource.getRandom(MAX, ANNOTATION)));
  }

  @Test
  void getRandomSingle_ShouldThrowExceptionWhenMaxIsNotPositive() {
    final Exception e =
        assertThrows(
            IllegalArgumentException.class, () -> plainRandomSource.getRandom(0, ANNOTATION));
    assertThat(e.getMessage(), containsString("max"));
  }

  @Test
  void getRandomMany_ShouldReturnRequestedCountOfValues() {
    assertThat(plainRandomSource.getRandom(MAX, 1, ANNOTATION).length, is(1));
    assertThat(plainRandomSource.getRandom(MAX, 42, ANNOTATION).length, is(42));
  }

  @Test
  void getRandomMany_ShouldReturnValuesBetweenZeroInclusiveAndMaxExclusive() {
    Arrays.stream(plainRandomSource.getRandom(MAX, 16, ANNOTATION))
        .forEach(PlainRandomSourceTest::assertValueBetweenZeroInclusiveAndMaxExclusive);
  }

  @Test
  void getRandomMany_ShouldThrowExceptionWhenMaxIsNotPositive() {

    final Exception e =
        assertThrows(
            IllegalArgumentException.class, () -> plainRandomSource.getRandom(0, 1, ANNOTATION));

    assertThat(e.getMessage(), containsString("max"));
  }

  @Test
  void getRandomMany_ShouldThrowExceptionWhenCountIsNotPositive() {
    final Exception e =
        assertThrows(
            IllegalArgumentException.class, () -> plainRandomSource.getRandom(MAX, 0, ANNOTATION));
    assertThat(e.getMessage(), containsString("count"));
  }
}
