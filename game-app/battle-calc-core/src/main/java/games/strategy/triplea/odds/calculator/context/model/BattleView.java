package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase1): the retreat decision may need more of the live state (checkpoint, terrain) than the
// two forces plus round.
/**
 * Read-only snapshot a {@link games.strategy.triplea.odds.calculator.context.seam.RetreatPolicy}
 * consults at a checkpoint.
 */
public record BattleView(Force attackers, Force defenders, int round) {}
