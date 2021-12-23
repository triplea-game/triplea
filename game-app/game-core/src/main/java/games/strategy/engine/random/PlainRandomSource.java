package games.strategy.engine.random;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/** A source of random numbers that uses a pseudorandom number generator. */
@ThreadSafe
public final class PlainRandomSource implements IRandomSource {
  private final Object lock = new Object();

  @GuardedBy("lock")
  private final RandomGenerator random = new MersenneTwister();

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) {
    checkArgument(max > 0, "max must be > 0 (%s)", annotation);
    checkArgument(count > 0, "count must be > 0 (%s)", annotation);

    final int[] numbers = new int[count];
    for (int i = 0; i < count; i++) {
      numbers[i] = getRandom(max, annotation);
    }
    return numbers;
  }

  @Override
  public int getRandom(final int max, final String annotation) {
    checkArgument(max > 0, "max must be > 0 (%s)", annotation);

    synchronized (lock) {
      return random.nextInt(max);
    }
  }
}
