package games.strategy.test;

import games.strategy.engine.random.IRandomSource;

/**
 * A random source for use while debugging.
 *
 *
 * <p>
 * When scripted random runs out of numbers, the numbers will repeat.
 * </p>
 *
 * <p>
 * Special characters are also allowed in the sequence.
 * e - the random source will throw an error p - the random source will pause and never return.
 * </p>
 */
public class ScriptedRandomSource implements IRandomSource {
  public static final int ERROR = -3;
  private final int[] numbers;
  private int currentIndex = 0;
  private int rolled;

  /**
   * Create a scripted random from the given numbers. The scripted random will return
   * the numbers supplied in order. When the scripted source runs out of random numbers, it
   * starts returning elements from the beginning.
   */
  public ScriptedRandomSource(final int... numbers) {
    this.numbers = numbers;
  }

  @Override
  public int getRandom(final int max, final String annotation) throws IllegalStateException {
    return getRandom(max, 1, null)[0];
  }

  @Override
  public int[] getRandom(final int max, final int count, final String annotation)
      throws IllegalArgumentException, IllegalStateException {
    if (count <= 0) {
      throw new IllegalArgumentException("count must be > 0, annotation:" + annotation);
    }
    rolled += count;
    final int[] numbers = new int[count];
    for (int i = 0; i < count; i++) {
      if (this.numbers[currentIndex] == ERROR) {
        throw new IllegalStateException("Random number generator generating scripted error");
      }
      numbers[i] = this.numbers[currentIndex];
      currentIndex++;
      if (currentIndex >= this.numbers.length) {
        currentIndex = 0;
      }
    }
    return numbers;
  }

  public int getTotalRolled() {
    return rolled;
  }
}
