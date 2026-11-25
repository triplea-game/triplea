package games.strategy.triplea.odds.calculator.context.model;

/** Orthogonal to {@link CombatProfile}: it never changes a unit's combat stats. */
public enum Lifecycle {
  ACTIVE,
  DEAD,
  WITHDRAWN
}
