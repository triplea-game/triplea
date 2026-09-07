package games.strategy.triplea.odds.calculator.context.model;

import java.util.EnumSet;

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
    EnumSet<CombatFlag> flags) {

  /**
   * The profile a unit becomes after one hit — the next-damage profile or the DEAD sentinel. The
   * transition is data the adapter bakes in (stat change or unit-type change), not a code branch.
   */
  public CombatProfile onHit() {
    // TODO(phase0-decision): needs the adapter-supplied transition; DEAD-sentinel representation is
    // deferred (a record has no natural null-profile). Throws until phase 1.
    throw new UnsupportedOperationException("phase 1");
  }
}
