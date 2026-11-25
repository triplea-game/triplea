package games.strategy.triplea.delegate.battle;

import java.io.Serializable;
import java.util.OptionalInt;

/**
 * Round-scoped cache of the single Low Luck remainder die that Same Luck shares across a battle
 * round. The first low-luck remainder roll of the round draws and caches a die; every later roll in
 * the same round — the other side's fire and any further firing groups — reuses it, so attacker and
 * defender share the same luck. {@link #reset()} is called at each round boundary.
 *
 * <p>Lives on {@link MustFightBattle}, which is serialized into save games and network-synced, so a
 * re-executed or resumed round reuses the same cached value rather than drawing a fresh one.
 */
public class SameLuckDiePool implements Serializable {
  private static final long serialVersionUID = 8823641029384756102L;

  // null until the round's first remainder roll caches a value
  private Integer die;
  // dice sides the cached value was drawn against; a group rolling different sides draws its own
  private int dieSides;

  /** The die cached this round for {@code diceSides}, or empty if none has been drawn yet. */
  public OptionalInt get(final int diceSides) {
    return die != null && dieSides == diceSides ? OptionalInt.of(die) : OptionalInt.empty();
  }

  public void set(final int diceSides, final int value) {
    this.die = value;
    this.dieSides = diceSides;
  }

  public void reset() {
    die = null;
  }
}
