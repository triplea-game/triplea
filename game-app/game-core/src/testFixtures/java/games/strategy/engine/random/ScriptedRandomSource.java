package games.strategy.engine.random;

/**
 * A deterministic {@link IRandomSource} for tests: it hands back dice values you dictate, so a
 * simulation's outcome no longer depends on real randomness.
 *
 * <p>Two modes cover the common needs:
 *
 * <ul>
 *   <li>A fixed script of values (via {@link #ScriptedRandomSource(int...)}) returned in order.
 *       Drawing past the end throws, so a test that under-scripts fails loudly instead of silently
 *       reading zeros.
 *   <li>Constant modes {@link #alwaysHits()} and {@link #alwaysMisses()} that never run out, for
 *       battles whose round count you don't want to predict.
 * </ul>
 *
 * <p>A die roll counts as a hit when its value is {@code < strength} (see {@code RolledDice}), so a
 * value of {@code 0} is a hit for any unit that can fire, and a value of {@code max - 1} is a miss
 * for any standard (d6, strength &le; 5) unit. {@link #alwaysHits()} and {@link #alwaysMisses()}
 * exploit exactly that.
 */
public final class ScriptedRandomSource implements IRandomSource {
  /** A die value that is a hit for any unit able to fire (strength is always &ge; 1). */
  public static final int HIT = 0;

  private enum Mode {
    SCRIPT,
    ALWAYS_HIT,
    ALWAYS_MISS
  }

  private final Mode mode;
  private final int[] script;
  private int position = 0;
  private int rollCount = 0;

  public ScriptedRandomSource(final int... script) {
    this.mode = Mode.SCRIPT;
    this.script = script.clone();
  }

  private ScriptedRandomSource(final Mode mode) {
    this.mode = mode;
    this.script = new int[0];
  }

  /** Every roll is a hit ({@value #HIT}), for any number of rolls. */
  public static ScriptedRandomSource alwaysHits() {
    return new ScriptedRandomSource(Mode.ALWAYS_HIT);
  }

  /** Every roll is a miss ({@code max - 1}), for any number of rolls. */
  public static ScriptedRandomSource alwaysMisses() {
    return new ScriptedRandomSource(Mode.ALWAYS_MISS);
  }

  /** The total number of dice drawn so far, across single- and batch-roll calls. */
  public int getRollCount() {
    return rollCount;
  }

  @Override
  public int getRandom(final int max, final String annotation) {
    rollCount++;
    return switch (mode) {
      case ALWAYS_HIT -> HIT;
      case ALWAYS_MISS -> max - 1;
      case SCRIPT -> {
        if (position >= script.length) {
          throw new IllegalStateException(
              "scripted dice exhausted after "
                  + script.length
                  + " rolls; the battle drew more dice than the script provided");
        }
        yield script[position++];
      }
    };
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
