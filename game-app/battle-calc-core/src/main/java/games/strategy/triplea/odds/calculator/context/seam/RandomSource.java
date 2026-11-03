package games.strategy.triplea.odds.calculator.context.seam;

/**
 * The core's own die source — keeps the calc engine-free; outer code bridges an engine {@code
 * IRandomSource} in through the adapter package.
 */
public interface RandomSource {
  int getRandom(int max, String annotation);

  int[] getRandom(int max, int count, String annotation);
}
