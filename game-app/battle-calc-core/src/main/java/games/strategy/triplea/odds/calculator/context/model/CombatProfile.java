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
    // The last round through which an IS_AA unit fires ('maxRoundsAa'; -1 = every round), mirroring
    // the engine's Matches.unitIsAaThatCanFireOnRound gate — a standard gun fires round 1 only.
    // Read only for AA profiles; baked -1 for every non-AA unit, which never consults it.
    int maxRoundsAa,
    // The raw per-gun AA dice cap ('getMaxAaAttacks'; -1 = infinite, else the finite per-gun roll
    // count). Static per unit-type, so identical guns still merge on this component; the per-round
    // total-dice cap against the live air-target count is applied at fire time, mirroring the
    // engine's AaPowerStrengthAndRolls. Non-AA units bake a constant -1, so no fungible profiles
    // split on it.
    int maxAaAttacks,
    int hitPoints,
    Domain domain,
    DamageState damage,
    SupportCategory gives,
    SupportCategory receives,
    // Effectively immutable: populated once at construction and only read thereafter, so the
    // profile
    // is safe to share across the simulator threads that read one scenario.
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
