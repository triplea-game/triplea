package games.strategy.triplea.odds.calculator.context.model;

import java.util.List;

/** The batch result: every run's {@link BattleResult}, with win/draw shares folded on demand. */
public record SimulationResults(List<BattleResult> results) {

  public double attackerWinPercent() {
    return percentOf(Outcome.ATTACKER_WINS);
  }

  public double defenderWinPercent() {
    return percentOf(Outcome.DEFENDER_WINS);
  }

  public double drawPercent() {
    return percentOf(Outcome.DRAW);
  }

  private double percentOf(final Outcome outcome) {
    // An empty batch has nothing to divide, so every share is 0.0 by convention.
    if (results.isEmpty()) {
      return 0.0;
    }
    final long matching = results.stream().filter(result -> result.outcome() == outcome).count();
    return (double) matching / results.size();
  }
}
