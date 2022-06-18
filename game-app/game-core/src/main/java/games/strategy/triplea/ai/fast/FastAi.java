package games.strategy.triplea.ai.fast;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ai.pro.AbstractProAi;
import games.strategy.triplea.ai.pro.ProData;

/** Fast AI. */
public class FastAi extends AbstractProAi {

  public FastAi(final String name, final String playerLabel) {
    this(name, new ProData(), playerLabel);
  }

  private FastAi(final String name, final ProData proData, final String playerLabel) {
    super(name, new FastOddsEstimator(proData), proData, playerLabel);
  }

  @Override
  protected void prepareData(final GameData data) {}
}
