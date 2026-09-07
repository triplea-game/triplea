package games.strategy.triplea.odds.calculator.context.model;

import java.util.Map;

/**
 * A partition of one side that fires together; profiles are already support-evaluated and
 * firingMode is dynamic per round.
 */
public record RollGroup(
    Side side,
    Map<CombatProfile, Integer> firing,
    TargetFilter target,
    FiringMode firingMode,
    DiceMode dice) {}
