package games.strategy.triplea.odds.calculator.context.model;

/**
 * One run's outcome; withdrawn units count as survivors, TUV is computed by the caller from cost.
 */
public record BattleResult(
    Force attackerSurvivors, Force defenderSurvivors, int roundsFought, Outcome outcome) {}
