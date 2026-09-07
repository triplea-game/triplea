package games.strategy.triplea.odds.calculator.context.model;

import java.util.List;

/**
 * Caller-supplied knobs for one calc run: the retreat rule and each side's order-of-loss
 * preference, which the adapter bakes into {@link BattleScenario}'s casualty orders.
 */
public record BattleOptions(
    boolean retreatWhenOnlyAirLeft, List<UnitTypeId> attackerOol, List<UnitTypeId> defenderOol) {}
