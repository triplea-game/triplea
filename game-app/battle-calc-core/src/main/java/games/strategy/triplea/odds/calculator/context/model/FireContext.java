package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase0-decision): design §11 flags that `round` may be redundant once the resolver bakes
// round-dependent bonuses into evaluated profiles — confirm before impl.
/**
 * The per-roll context a {@link games.strategy.triplea.odds.calculator.context.seam.HitRoller}
 * needs; unit-specific flags live on the profile.
 */
public record FireContext(
    int round, Phase phase, boolean isOffense, boolean lowLuck, int diceSides) {}
