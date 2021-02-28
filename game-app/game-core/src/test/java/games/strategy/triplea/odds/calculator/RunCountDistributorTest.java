package games.strategy.triplea.odds.calculator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

public class RunCountDistributorTest {

  @Test
  void verifyEvenDistributionWhenCleanlyDividable() {
    final var runCountDistributor = new RunCountDistributor(20, 4);

    assertThat(runCountDistributor.nextRunCount(), is(equalTo(5)));
    assertThat(runCountDistributor.nextRunCount(), is(equalTo(5)));
    assertThat(runCountDistributor.nextRunCount(), is(equalTo(5)));
    assertThat(runCountDistributor.nextRunCount(), is(equalTo(5)));
  }

  @Test
  void verifyDistributionWhenNotCleanlyDividable() {
    final var runCountDistributor = new RunCountDistributor(13, 3);

    assertThat(runCountDistributor.nextRunCount(), is(equalTo(5)));
    assertThat(runCountDistributor.nextRunCount(), is(equalTo(4)));
    assertThat(runCountDistributor.nextRunCount(), is(equalTo(4)));
  }

  @Test
  void verifyDistributionWhenParallelismTooHigh() {
    final var runCountDistributor = new RunCountDistributor(1, 3);

    assertThat(runCountDistributor.nextRunCount(), is(equalTo(1)));
    assertThat(runCountDistributor.nextRunCount(), is(equalTo(0)));
    assertThat(runCountDistributor.nextRunCount(), is(equalTo(0)));
  }

  @Test
  void verifyExceptionWhenUsingInvalidParallelism() {
    assertThrows(IllegalStateException.class, () -> new RunCountDistributor(1, 0));
    assertThrows(IllegalStateException.class, () -> new RunCountDistributor(1, -20));
  }

  @Test
  void verifyExceptionWhenCallingNextRunCountTooOften() {
    final var runCountDistributor = new RunCountDistributor(1, 1);

    assertThat(runCountDistributor.nextRunCount(), is(equalTo(1)));

    assertThrows(IllegalStateException.class, runCountDistributor::nextRunCount);
  }

  /**
   * Obviously this test isn't guaranteed to fail if {@link RunCountDistributor} is not actually
   * thread-safe, but in case is does fail we have a bad implementation.
   */
  @Test
  void verifyParallelExecutionWorksWithoutException() {
    final int parallelism = 64;
    final int runCount = 1337;
    final var runCountDistributor = new RunCountDistributor(runCount, parallelism);

    final int summedRunCount =
        IntStream.range(0, parallelism)
            .parallel()
            .map(i -> runCountDistributor.nextRunCount())
            .sum();

    assertThat(summedRunCount, is(runCount));
  }
}
