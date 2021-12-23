package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.odds.calculator.ConcurrentBattleCalculator;

public class ProAi extends AbstractProAi {
  // Odds calculator
  private static final ConcurrentBattleCalculator concurrentCalc = new ConcurrentBattleCalculator();

  public ProAi(final String name) {
    super(name, concurrentCalc, new ProData());
    // cuncurrentCalc is static so that it can be shared across all ProAi instances
    // at the end of a game, it needs to be cleared up
    GameShutdownRegistry.registerShutdownAction(() -> concurrentCalc.setGameData(null));
  }

  @Override
  public PlayerTypes.Type getPlayerType() {
    return PlayerTypes.PRO_AI;
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
