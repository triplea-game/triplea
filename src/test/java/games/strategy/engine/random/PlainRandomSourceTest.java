package games.strategy.engine.random;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.Test;

public final class PlainRandomSourceTest {
  private static final String ANNOTATION = "annotation";
  private static final int MAX = 6;

  private final PlainRandomSource plainRandomSource = new PlainRandomSource();

  private static void assertValueBetweenZeroInclusiveAndMaxExclusive(final int value) {
    assertThat(value, allOf(greaterThanOrEqualTo(0), lessThan(MAX)));
  }

  @Test
  public void getRandomSingle_ShouldReturnValueBetweenZeroInclusiveAndMaxExclusive() {
    IntStream.range(0, 5_000)
        .forEach(i -> assertValueBetweenZeroInclusiveAndMaxExclusive(plainRandomSource.getRandom(MAX, ANNOTATION)));
  }

  @Test
  public void getRandomSingle_ShouldThrowExceptionWhenMaxIsNotPositive() {
    catchException(() -> plainRandomSource.getRandom(0, ANNOTATION));

    assertThat(caughtException(), allOf(
        is(instanceOf(IllegalArgumentException.class)),
        hasMessageThat(containsString("max"))));
  }

  @Test
  public void getRandomMany_ShouldReturnRequestedCountOfValues() {
    assertThat(plainRandomSource.getRandom(MAX, 1, ANNOTATION).length, is(1));
    assertThat(plainRandomSource.getRandom(MAX, 42, ANNOTATION).length, is(42));
  }

  @Test
  public void getRandomMany_ShouldReturnValuesBetweenZeroInclusiveAndMaxExclusive() {
    Arrays.stream(plainRandomSource.getRandom(MAX, 16, ANNOTATION))
        .forEach(PlainRandomSourceTest::assertValueBetweenZeroInclusiveAndMaxExclusive);
  }

  @Test
  public void getRandomMany_ShouldThrowExceptionWhenMaxIsNotPositive() {
    catchException(() -> plainRandomSource.getRandom(0, 1, ANNOTATION));

    assertThat(caughtException(), allOf(
        is(instanceOf(IllegalArgumentException.class)),
        hasMessageThat(containsString("max"))));
  }

  @Test
  public void getRandomMany_ShouldThrowExceptionWhenCountIsNotPositive() {
    catchException(() -> plainRandomSource.getRandom(MAX, 0, ANNOTATION));

    assertThat(caughtException(), allOf(
        is(instanceOf(IllegalArgumentException.class)),
        hasMessageThat(containsString("count"))));
  }
}
