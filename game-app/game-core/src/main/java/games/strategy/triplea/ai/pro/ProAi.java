package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.triplea.odds.calculator.ConcurrentBattleCalculator;

public class ProAi extends AbstractProAi {
  // Odds calculator
  private static final ConcurrentBattleCalculator concurrentCalc = new ConcurrentBattleCalculator();

  public ProAi(final String name, final String playerLabel) {
    super(name, concurrentCalc, new ProData(), playerLabel);
    // cuncurrentCalc is static so that it can be shared across all ProAi instances
    // at the end of a game, it needs to be cleared up
    GameShutdownRegistry.registerShutdownAction(() -> concurrentCalc.setGameData(null));
  }

  @Override
  public void stopGame() {
    super.stopGame(); // absolutely MUST call super.stopGame() first
    concurrentCalc.cancel();
    concurrentCalc.setGameData(null);
  }

  @Override
  protected void prepareData(final GameData data) {
    concurrentCalc.setGameData(data);
  }
}
