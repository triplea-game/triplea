package games.strategy.triplea.ai.tree;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.ai.pro.AbstractProAi;
import games.strategy.triplea.ai.pro.ProData;

/** Battle Tree AI. */
public class BattleTreeAi extends AbstractProAi {

  private static final BattleTreeCalculator calc = new BattleTreeCalculator();

  public BattleTreeAi(final String name) {
    this(name, new ProData());
  }

  private BattleTreeAi(final String name, final ProData proData) {
    super(name, calc, proData);
  }

  @Override
  public PlayerType getPlayerType() {
    return PlayerType.BATTLE_TREE_AI;
  }

  @Override
  protected void prepareData(final GameData data) {
    calc.setGameData(data);
  }
}
