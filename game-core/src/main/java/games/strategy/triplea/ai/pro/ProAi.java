package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.ai.pro.logging.ProLogUi;
import games.strategy.triplea.odds.calculator.ConcurrentBattleCalculator;

public class ProAi extends AbstractProAi {
  // Odds calculator
  private static final ConcurrentBattleCalculator concurrentCalc =
      new ConcurrentBattleCalculator("ProAi");

  public ProAi(final String name) {
    super(name, concurrentCalc, new ProData());
  }

  public static void gameOverClearCache() {
    // Are static, clear so that we don't keep the data around after a game is exited
    concurrentCalc.setGameData(null);
    ProLogUi.clearCachedInstances();
  }

  @Override
  public PlayerType getPlayerType() {
    return PlayerType.PRO_AI;
  }

  @Override
  public void stopGame() {
    super.stopGame(); // absolutely MUST call super.stopGame() first
    concurrentCalc.cancel();
  }

  @Override
  protected void prepareData(final GameData data) {
    concurrentCalc.setGameData(data);
  }
}
