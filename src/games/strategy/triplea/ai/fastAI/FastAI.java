package games.strategy.triplea.ai.fastAI;

import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;

/**
 * Fast AI.
 */
public class FastAI extends ProAI {

  public FastAI(final String name, final String type) {
    super(name, type);
  }

  @Override
  public void initializeBattleCalculator() {
    final IOddsCalculator calc = new FastOddsEstimator();
    ProBattleUtils.setOddsCalculator(calc);
  }

}
