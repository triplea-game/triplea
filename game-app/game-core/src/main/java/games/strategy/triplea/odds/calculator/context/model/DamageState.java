package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase1): a bare hit count may need to carry maxDamage / operational-damage bounds once
// multi-HP and whenHitPointsDamagedChangesInto are ported.
/** How many hits a unit has absorbed; combat-relevant, so it lives in {@link CombatProfile}. */
public record DamageState(int hitsTaken) {}
