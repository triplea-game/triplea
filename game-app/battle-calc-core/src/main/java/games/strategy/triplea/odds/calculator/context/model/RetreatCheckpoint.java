package games.strategy.triplea.odds.calculator.context.model;

/** The point in a round a {@code RetreatPolicy} is consulted; different rules fire at each. */
public enum RetreatCheckpoint {
  SUBMERGE,
  END_OF_ROUND
}
