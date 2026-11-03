package games.strategy.triplea.odds.calculator.context.model;

/**
 * The context-local stand-in for the engine's {@code UnitType}: identity for cost/display remap and
 * dependent resolution, carried without any engine.data reference.
 */
public record UnitTypeId(String name) {}
