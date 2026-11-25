package games.strategy.triplea.odds.calculator.context.model;

import java.util.Map;

/** Cargo rules beside the {@link Force}; drives the dependent cascade in the allocator. */
public record Dependents(Map<CombatProfile, CargoRule> rules) {}
