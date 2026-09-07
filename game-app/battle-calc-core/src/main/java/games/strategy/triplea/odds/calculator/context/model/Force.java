package games.strategy.triplea.odds.calculator.context.model;

import java.util.Map;

/** One side as raw profile counts — the whole hot-path state vector. */
public record Force(Map<Key, Integer> counts) {}
