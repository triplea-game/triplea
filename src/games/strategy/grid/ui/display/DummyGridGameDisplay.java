package games.strategy.grid.ui.display;

import java.util.Collection;

import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;

/**
 *
 * @author veqryn
 *
 */
public class DummyGridGameDisplay implements IGridGameDisplay {
  private final MainGameFrame m_ui;

  /**
   * A display which does absolutely nothing
   */
  public DummyGridGameDisplay() {
    m_ui = null;
  }

  /**
   * A display which does absolutely nothing, except for stopping the game on shutdown.
   *
   * @param ui
   *        MainGameFrame which we will call .stopGame() on if this DummyGridGameDisplay has .shutDown() called.
   */
  public DummyGridGameDisplay(final MainGameFrame ui) {
    m_ui = ui;
  }

  @Override
  public void initialize(final IDisplayBridge bridge) {}

  @Override
  public void shutDown() {
    // make sure to shut down the ui if there is one
    if (m_ui != null) {
      m_ui.stopGame();
    }
  }

  @Override
  public void setStatus(final String status) {}

  @Override
  public void setGameOver() {}

  @Override
  public void refreshTerritories(final Collection<Territory> territories) {}

  @Override
  public void showGridPlayDataMove(final IGridPlayData move) {}

  @Override
  public void showGridEndTurnData(final IGridEndTurnData endTurnData) {}

  @Override
  public void initializeGridMapData(final GameMap map) {}

  @Override
  public GridGameFrame getGridGameFrame() {
    return null;
  }
}
