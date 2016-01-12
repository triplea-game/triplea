package games.strategy.engine.random;

/**
 * Gets random numbers from javas random number generators.
 */
public class PlainRandomSource implements IRandomSource {
  /**
   * Knowing the seed gives a player an advantage.
   * Do something a little more clever than current time.
   * which could potentially be guessed
   * If the execution path is different before the first random
   * call is made then the object will have a somewhat random
   * adress in the virtual machine, especially if
   * a lot of ui and networking objects are created
   * in response to semi random mouse motion etc.
   * if the excecution is always the same then
   * this may vary depending on the vm
   */
  private static long getSeed() {
    final Object seedObj = new Object();
    // hash code is an int, 32 bits
    long seed = seedObj.hashCode();
    seed += System.currentTimeMillis();
    // seed with current time as well
    seed += System.nanoTime();
    return seed;
  }


  private static MersenneTwister s_random;

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
    if (s_random == null) {
      s_random = new MersenneTwister(getSeed());
    }
    return s_random.nextInt(max);
  }
}
