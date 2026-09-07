package games.strategy.triplea.odds.calculator.context.model;

import java.util.EnumSet;
import java.util.Optional;

/**
 * The board-counter identity: the pure combat stats a unit carries into battle, and the merge key
 * for the state vector — equality means combat-fungible. A property lives here iff it changes
 * combat behavior, so damage is in but lifecycle is not.
 */
public record CombatProfile(
    UnitTypeId type,
    int attack,
    int defense,
    int rolls,
    int hitPoints,
    Domain domain,
    DamageState damage,
    SupportCategory gives,
    SupportCategory receives,
    EnumSet<CombatFlag> flags,
    // The profile this one migrates to on a hit, or null if the hit kills it; encodes
    // whenHitPointsDamagedChangesInto (a stat or unit-type change) as data. Combat-relevant, so it
    // participates in equality (invariant 1) — a same-stats unit with a different successor is a
    // different bucket.
    CombatProfile next) {

  /** The profile a unit becomes after one hit, or empty when the hit kills it. */
  public Optional<CombatProfile> onHit() {
    return Optional.ofNullable(next);
  }
}
