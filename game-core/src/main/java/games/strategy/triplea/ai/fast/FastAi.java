package games.strategy.triplea.ai.fast;

import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;

/**
 * Fast AI.
 */
public class FastAi extends ProAi {

  // Odds estimator
  private static final IOddsCalculator estimator = new FastOddsEstimator();

  public FastAi(final String name, final String type) {
    super(name, type);
  }

  @Override
  protected void initializeCalc() {
    calc = new ProOddsCalculator(estimator);
  }

}
