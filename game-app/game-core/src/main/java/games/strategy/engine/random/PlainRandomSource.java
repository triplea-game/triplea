package games.strategy.engine.random;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/** A source of random numbers that uses a pseudorandom number generator. */
@ThreadSafe
public final class PlainRandomSource implements IRandomSource {
  private final Object lock = new Object();

  // ThreadLocalRandom provides well-distributed seeds even when many instances are created
  // concurrently (e.g. AI battle simulators). MersenneTwister's no-arg constructor seeds from
  // currentTimeMillis + identityHashCode, which can collide within the same millisecond and cause
  // multiple simulators to produce identical dice sequences.
  @GuardedBy("lock")
  private final RandomGenerator random = new MersenneTwister(ThreadLocalRandom.current().nextLong());

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
