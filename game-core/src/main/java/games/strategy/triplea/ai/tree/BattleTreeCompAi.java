package games.strategy.triplea.ai.tree;

    import games.strategy.engine.data.GameData;
    import games.strategy.engine.framework.startup.ui.PlayerType;
    import games.strategy.triplea.ai.pro.AbstractProAi;
    import games.strategy.triplea.ai.pro.ProData;
    import games.strategy.triplea.ai.pro.logging.ProLogUi;

/** Battle Tree AI. */
public class BattleTreeCompAi extends AbstractProAi {

  private static final BattleTreeCompCalculator calc = new BattleTreeCompCalculator();

  public BattleTreeCompAi(final String name) {
    this(name, new ProData());
  }

  private BattleTreeCompAi(final String name, final ProData proData) {
    super(name, calc, proData);
  }

  @Override
  public PlayerType getPlayerType() {
    return PlayerType.BATTLE_TREE_AI;
  }

  public static void gameOverClearCache() {
    // Are static, clear so that we don't keep the data around after a game is exited
    calc.setGameData(null);
    ProLogUi.clearCachedInstances();
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
