package games.strategy.triplea.ai.fast;

import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;

/** Fast AI. */
public class FastAi extends ProAi {

  public FastAi(final String name) {
    super(name);
  }

  @Override
  protected void initializeCalc() {
    calc = new ProOddsCalculator(new FastOddsEstimator(getProData()));
  }

  @Override
  public PlayerType getPlayerType() {
    return PlayerType.FAST_AI;
  }
}
