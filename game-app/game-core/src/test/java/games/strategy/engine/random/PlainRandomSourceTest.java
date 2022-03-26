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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.triplea.java.ThreadRunner;

final class PlainRandomSourceTest {
  private static final String ANNOTATION = "annotation";
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
    for (int count : new int[] {1, 41, 42}) {
      assertThat(plainRandomSource.getRandom(MAX, count, ANNOTATION).length, is(count));
    }
  }

  @Test
  void getRandomMany_Performance() {
    final long maxRuntimeNano = 100000000; // 100ms
    final int testRuns = 1000;
    final int diceCount = 400;
    long startTime = System.nanoTime();
    for (int i = 0; i < testRuns; ++i) {
      int[] number = plainRandomSource.getRandom(MAX, diceCount, ANNOTATION);
    }
    final long stopTime = System.nanoTime();

    assertThat(maxRuntimeNano, Matchers.greaterThanOrEqualTo(stopTime - startTime));
  }

  @Test
  void getRandomMany_MultipleThreads() {
    final int threadCount = 35;
    for (int t = 0; t < threadCount; t++) {
      ThreadRunner.runInNewThread(this::getRandomMany_Performance);
    }
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
