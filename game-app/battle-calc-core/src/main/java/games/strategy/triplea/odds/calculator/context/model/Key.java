package games.strategy.triplea.odds.calculator.context.model;

/**
 * The state-vector key: a profile in a lifecycle. Every combat transition is a migration between
 * keys.
 */
public record Key(CombatProfile profile, Lifecycle state) {}
