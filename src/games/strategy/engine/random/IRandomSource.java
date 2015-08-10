package games.strategy.engine.random;

/**
 * A source for random numbers.
 */
public interface IRandomSource {
  public int getRandom(int max, String annotation) throws IllegalArgumentException, IllegalStateException;

  public int[] getRandom(int max, int count, String annotation) throws IllegalArgumentException, IllegalStateException;
}
