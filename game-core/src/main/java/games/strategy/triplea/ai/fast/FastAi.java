package games.strategy.triplea.ai.fast;

import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.odds.calculator.IOddsCalculator;

/**
 * Fast AI.
 */
public class FastAi extends ProAi {

  // Odds estimator
  private static final IOddsCalculator estimator = new FastOddsEstimator();

  public FastAi(final String name) {
    super(name);
  }

  @Override
  protected void initializeCalc() {
    calc = new ProOddsCalculator(estimator);
  }

  @Override
  public PlayerType getPlayerType() {
    return PlayerType.FAST_AI;
  }
}
