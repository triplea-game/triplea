package games.strategy.engine.random;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.math3.random.MersenneTwister;

/**
 * A source of random numbers that uses a pseudorandom number generator.
 */
@ThreadSafe
public final class PlainRandomSource implements IRandomSource {
  private static final Object classLock = new Object();

  @GuardedBy("classLock")
  private static MersenneTwister random;

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) {
    checkArgument(max > 0, String.format("max must be > 0 (%s)", annotation));
    checkArgument(count > 0, String.format("count must be > 0 (%s)", annotation));

    final int[] numbers = new int[count];
    for (int i = 0; i < count; i++) {
      numbers[i] = getRandom(max, annotation);
    }
    return numbers;
  }

  @Override
  public int getRandom(final int max, final String annotation) {
    checkArgument(max > 0, String.format("max must be > 0 (%s)", annotation));

    synchronized (classLock) {
      if (random == null) {
        random = new MersenneTwister();
      }

      return random.nextInt(max);
    }
  }
}
