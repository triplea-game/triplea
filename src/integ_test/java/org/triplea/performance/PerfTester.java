package org.triplea.performance;


import org.apache.commons.math3.util.IntegerSequence;

import com.google.common.base.Preconditions;

public class PerfTester {

  private PerfTester(final Runnable runnable, final int runCount, final int maxRuntimeMillis) {

    final long start = System.nanoTime();

    IntegerSequence.range(1, runCount).forEach(i -> runnable.run());

    final long duration = nanoToMillis(System.nanoTime() - start);

    Preconditions.checkState(
        duration < maxRuntimeMillis,
        String.format("Duration: %s greater than max allowed %s",
            duration, maxRuntimeMillis));
  }

  private static long nanoToMillis(final long nano) {
    return nano / (1000 * 1000);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Runnable runnable;
    private int runCount;
    private int maxRuntimeMs;

    public Builder runnable(final Runnable runnable) {
      this.runnable = runnable;
      return this;
    }

    public Builder runCount(final int runCount) {
      this.runCount = runCount;
      return this;
    }

    public Builder maxRunTime(final int maxRuntimeMs) {
      this.maxRuntimeMs = maxRuntimeMs;
      return this;
    }

    public PerfTester runTest() {
      Preconditions.checkState(runCount > 0);
      Preconditions.checkState(maxRuntimeMs > 0);
      Preconditions.checkNotNull(runnable);

      return new PerfTester(runnable, runCount, maxRuntimeMs);
    }
  }
}
