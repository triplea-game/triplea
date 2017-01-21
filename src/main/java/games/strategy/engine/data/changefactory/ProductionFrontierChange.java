package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;

/**
 * Change a players production frontier.
 */
class ProductionFrontierChange extends Change {
  private final String m_startFrontier;
  private final String m_endFrontier;
  private final String m_player;
  private static final long serialVersionUID = 3336145814067456701L;

  ProductionFrontierChange(final ProductionFrontier newFrontier, final PlayerID player) {
    m_startFrontier = player.getProductionFrontier().getName();
    m_endFrontier = newFrontier.getName();
    m_player = player.getName();
  }

  ProductionFrontierChange(final String startFrontier, final String endFrontier, final String player) {
    m_startFrontier = startFrontier;
    m_endFrontier = endFrontier;
    m_player = player;
  }

  @Override
  protected void perform(final GameData data) {
    final PlayerID player = data.getPlayerList().getPlayerID(m_player);
    final ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier(m_endFrontier);
    player.setProductionFrontier(frontier);
  }

  @Override
  public Change invert() {
    return new ProductionFrontierChange(m_endFrontier, m_startFrontier, m_player);
  }
}
