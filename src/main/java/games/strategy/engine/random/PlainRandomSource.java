package games.strategy.engine.random;

import org.apache.commons.math3.random.MersenneTwister;

/**
 * A source of random numbers that uses a pseudorandom number generator.
 */
public class PlainRandomSource implements IRandomSource {
  private static MersenneTwister random;

  @Override
  public synchronized int[] getRandom(final int max, final int count, final String annotation)
      throws IllegalArgumentException {
    if (count <= 0) {
      throw new IllegalArgumentException("count must be > 0, annotation:" + annotation);
    }
    final int[] numbers = new int[count];
    for (int i = 0; i < count; i++) {
      numbers[i] = getRandom(max, annotation);
    }
    return numbers;
  }

  @Override
  public synchronized int getRandom(final int max, final String annotation) throws IllegalArgumentException {
    if (random == null) {
      random = new MersenneTwister();
    }
    return random.nextInt(max);
  }
}
