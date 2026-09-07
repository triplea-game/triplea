package games.strategy.triplea.odds.calculator.context;

import games.strategy.triplea.odds.calculator.context.seam.RandomSource;

/**
 * Engine-free {@link RandomSource} for the core-seam tests. A die hits when its value is {@code <
 * strength}, so {@link #alwaysHits()} rolls 0 for an unbounded run; {@link #scripted} replays exact
 * values in order, which the engine's {@code ScriptedRandomSource} cannot do (it only knows
 * alwaysHits).
 */
public final class FakeRandomSource implements RandomSource {
  private final int[] values;
  private final boolean repeatLast;
  private int next = 0;

  private FakeRandomSource(final int[] values, final boolean repeatLast) {
    this.values = values;
    this.repeatLast = repeatLast;
  }

  /** Every roll returns 0, so every die hits, for any number of rolls. */
  public static FakeRandomSource alwaysHits() {
    return new FakeRandomSource(new int[] {0}, true);
  }

  /** Replays the given die values in order, one per roll; over-drawing is a test error. */
  public static FakeRandomSource scripted(final int... values) {
    return new FakeRandomSource(values, false);
  }

  @Override
  public int getRandom(final int max, final String annotation) {
    if (repeatLast && next >= values.length) {
      return values[values.length - 1];
    }
    return values[next++];
  }

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) {
    final int[] result = new int[count];
    for (int i = 0; i < count; i++) {
      result[i] = getRandom(max, annotation);
    }
    return result;
  }
}
