package games.strategy.triplea.ai.fastAI;

import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.util.ProOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;

/**
 * Fast AI.
 */
public class FastAI extends ProAI {

  // Odds estimator
  private final static IOddsCalculator estimator = new FastOddsEstimator();

  public FastAI(final String name, final String type) {
    super(name, type);
  }

  @Override
  protected void initializeCalc() {
    calc = new ProOddsCalculator(estimator);
  }

}
