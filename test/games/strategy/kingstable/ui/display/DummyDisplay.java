package games.strategy.kingstable.ui.display;

import java.util.Collection;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.grid.ui.display.IGridGameDisplay;

/**
 * Dummy display for a King's Table game, for use in testing.
 */
public class DummyDisplay implements IGridGameDisplay {
  @Override
  public void refreshTerritories(final Collection<Territory> territories) {}

  @Override
  public void setGameOver()// CountDownLatch waiting) {
  {}

  @Override
  public void setStatus(final String status) {}

  @Override
  public void initialize(final IDisplayBridge bridge) {}

  @Override
  public void shutDown() {}

  @Override
  public void initializeGridMapData(final GameMap map) {}

  @Override
  public GridGameFrame getGridGameFrame() {
    return null;
  }

  @Override
  public void showGridPlayDataMove(final IGridPlayData move) {}

  @Override
  public void showGridEndTurnData(final IGridEndTurnData endTurnData) {}
}
