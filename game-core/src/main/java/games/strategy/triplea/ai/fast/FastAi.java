package games.strategy.triplea.ai.fast;

import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.odds.calculator.IBattleCalculator;

/** Fast AI. */
public class FastAi extends ProAi {

  // Odds estimator
  private final IBattleCalculator estimator;

  public FastAi(final String name) {
    super(name);
    estimator = new FastOddsEstimator(getProData());
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
