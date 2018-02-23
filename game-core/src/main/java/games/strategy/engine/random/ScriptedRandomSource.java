package games.strategy.engine.random;

import java.util.StringTokenizer;

/**
 * A random source for use while debugging.
 *
 * <p>
 * Returns the random numbers designated in the system property triplea.scriptedRandom
 * </p>
 *
 * <p>
 * for example, to roll 1,2,3 use -Dtriplea.scriptedRandom=1,2,3
 * </p>
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
  public static final int PAUSE = -2;
  public static final int ERROR = -3;
  private static final String SCRIPTED_RANDOM_PROPERTY = "triplea.scriptedRandom";
  private final int[] numbers;
  private int currentIndex = 0;
  private int rolled;

  /**
   * Should we use a scripted random sourcce.
   */
  public static boolean useScriptedRandom() {
    return (System.getProperty(SCRIPTED_RANDOM_PROPERTY) != null)
        && (System.getProperty(SCRIPTED_RANDOM_PROPERTY).trim().length() > 0);
  }

  /**
   * Create a scripted random source from the system property triplea.scriptedRandom.
   */
  public ScriptedRandomSource() {
    final String property = System.getProperty(SCRIPTED_RANDOM_PROPERTY, "1,2,3");
    final int length = property.split(",").length;
    final StringTokenizer tokenizer = new StringTokenizer(property, ",");
    numbers = new int[length];
    for (int i = 0; i < numbers.length; i++) {
      final String token = tokenizer.nextToken();
      if (token.equals("e")) {
        numbers[i] = ERROR;
      } else if (token.equals("p")) {
        numbers[i] = PAUSE;
      } else {
        numbers[i] = Integer.parseInt(token) - 1;
      }
    }
  }

  /**
   * Create a scripted random from the given numbers. The scripted random will return
   * the numbers supplied in order. When the scripted source runs out of random numbers, it
   * starts returning elements from the beginning.
   */
  public ScriptedRandomSource(final int[] numbers) {
    this.numbers = numbers;
  }

  public ScriptedRandomSource(final Integer... numbers) {
    this.numbers = new int[numbers.length];
    for (int i = 0; i < numbers.length; i++) {
      this.numbers[i] = numbers[i];
    }
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
