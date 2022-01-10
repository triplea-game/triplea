package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator;

public class ProAi extends AbstractProAi {
  // Odds calculator
  private static final StochasticBattleCalculator calc = new StochasticBattleCalculator();

  public ProAi(final String name) {
    super(name, calc, new ProData());
    // cuncurrentCalc is static so that it can be shared across all ProAi instances
    // at the end of a game, it needs to be cleared up
    GameShutdownRegistry.registerShutdownAction(() -> calc.setGameData(null));
  }

  @Override
  public PlayerTypes.Type getPlayerType() {
    return PlayerTypes.PRO_AI;
  }

  @Override
  public void stopGame() {
    super.stopGame(); // absolutely MUST call super.stopGame() first
    calc.cancel();
  }

  @Override
  protected void prepareData(final GameData data) {
    calc.setGameData(data);
  }
}
