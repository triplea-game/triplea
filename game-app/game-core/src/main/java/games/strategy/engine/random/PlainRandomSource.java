package games.strategy.engine.random;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.SplittableRandom;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

/** A source of random numbers that uses a pseudorandom number generator. */
@ThreadSafe
public final class PlainRandomSource implements IRandomSource {
  private final Object lock = new Object();

  /// SplittableRandom (AKA splitmix64) is selected for reasons:
  /// - small state (8 bytes)
  /// - good cache performance under concurrent simulation
  /// - passes BigCrush,
  /// - no LSB quality issues
  /// Documentation:
  /// - https://docs.oracle.com/javase/8/docs/api/java/util/SplittableRandom.html
  ///
  @GuardedBy("lock")
  private final SplittableRandom random = new SplittableRandom();

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
