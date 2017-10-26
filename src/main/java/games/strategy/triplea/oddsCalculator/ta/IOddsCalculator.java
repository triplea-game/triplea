package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.triplea.oddscalc.OddsCalculatorParameters;

/**
 * Interface to ensure different implementations of the odds calculator all have the same public methods.
 */
public interface IOddsCalculator {
  AggregateResults calculate(OddsCalculatorParameters parameters);

  void cancel();
}
