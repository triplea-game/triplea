package games.strategy.triplea.odds.calculator;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Helper class to divide any integer into smaller integers that are the original integer when
 * summed up.
 */
@ThreadSafe
class RunCountDistributor {
  private final int runsPerWorker;
  private final int targetLeftover;
  private final AtomicInteger leftoverRuns;

  /**
   * Creates a new RunCountDistributorInstance.
   *
   * @param runCount What all of the individual run-counts should sum up to
   * @param parallelism How many times {@link #nextRunCount()} will get called.
   */
  RunCountDistributor(final int runCount, final int parallelism) {
    Preconditions.checkState(parallelism > 0, "The parallelism level has to be positive!");

    runsPerWorker = runCount / parallelism;
    leftoverRuns = new AtomicInteger(runCount % parallelism);
    targetLeftover = leftoverRuns.get() - parallelism;
  }

  /**
   * Returns the next run-count.
   *
   * @throws IllegalStateException If this method was called more often than provided level of
   *     parallelism.
   */
  int nextRunCount() {
    final int leftoverRuns = this.leftoverRuns.getAndDecrement();
    if (leftoverRuns <= targetLeftover) {
      throw new IllegalStateException(
          "nextRunCount() was called more times than specified by provided level of parallelism");
    }
    return (leftoverRuns > 0 ? 1 : 0) + runsPerWorker;
  }
}
