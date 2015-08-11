package games.strategy.grid.ui.display;

import java.util.Collection;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplay;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;

public interface IGridGameDisplay extends IDisplay {
  /**
   * Graphically notify the user of the current game status.
   *
   * @param error
   *        the status message to display
   */
  public void setStatus(String status);

  /**
   * Set the game over status for this display to <code>true</code>.
   */
  public void setGameOver();// CountDownLatch waiting);

  /**
   * Ask the user interface for this display to process a play and zero or more captures.
   *
   * @param territories
   *        <code>Collection</code> of <code>Territory</code>s whose pieces have changed
   */
  public void refreshTerritories(Collection<Territory> territories);

  public void showGridPlayDataMove(IGridPlayData move);

  public void showGridEndTurnData(IGridEndTurnData endTurnData);

  /**
   * Initialize the board.
   */
  public void initializeGridMapData(GameMap map);

  public GridGameFrame getGridGameFrame();
}
