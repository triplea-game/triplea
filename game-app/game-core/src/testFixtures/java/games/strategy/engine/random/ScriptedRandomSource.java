package games.strategy.engine.random;

/**
 * A deterministic {@link IRandomSource} for tests: it removes the dice from a simulation so the
 * outcome is decided by the rules alone.
 *
 * <p>A die roll counts as a hit when its value is {@code < strength} (see {@code RolledDice}), so a
 * value of {@code 0} is a hit for every unit that can fire. {@link #alwaysHits()} returns {@code 0}
 * for every roll, which drives a battle to a rules-determined result: each side's fire always
 * connects, so survivors depend only on unit counts and casualty selection, never on luck.
 */
public final class ScriptedRandomSource implements IRandomSource {
  private static final int HIT = 0;

  private ScriptedRandomSource() {}

  /** A source whose every roll is a hit, for any number of rolls. */
  public static ScriptedRandomSource alwaysHits() {
    return new ScriptedRandomSource();
  }

  @Override
  public int getRandom(final int max, final String annotation) {
    return HIT;
  }

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) {
    final int[] numbers = new int[count];
    for (int i = 0; i < count; i++) {
      numbers[i] = getRandom(max, annotation);
    }
    return numbers;
  }
}
