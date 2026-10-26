package games.strategy.triplea.odds.calculator.context.model;

import java.util.Map;

/**
 * A partition of one side that fires together; profiles are already support-evaluated and
 * firingMode is dynamic per round. {@code phase} is when it rolls; {@code firingMode} is when its
 * casualties land — IMMEDIATE before the next phase, DEFERRED only after the opposing main combat
 * has fired (a WW2V2 destroyer-pinned sub rolls in the sub phase but its victims still fire back).
 */
public record RollGroup(
    Side side,
    Map<CombatProfile, Integer> firing,
    TargetFilter target,
    FiringMode firingMode,
    DiceMode dice,
    Phase phase) {}
