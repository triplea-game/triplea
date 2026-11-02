package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase0-decision): phase set is interpreted from the pipeline (AA -> first strike -> main);
// confirm the full enumeration and whether bombard is a distinct phase.
/** The combat phase a firing group belongs to; sequences the per-round plan. */
public enum Phase {
  AA,
  BOMBARD,
  FIRST_STRIKE,
  GENERAL
}
