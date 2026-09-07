package games.strategy.triplea.odds.calculator.context.model;

import java.util.SequencedMap;
import java.util.Set;

/**
 * The per-round firing plan; key order is the firing sequence, value is the groups each one
 * targets.
 */
public record BattleRound(SequencedMap<RollGroup, Set<RollGroup>> firing) {}
