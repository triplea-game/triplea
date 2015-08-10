package games.strategy.grid.ui.display;

import java.util.Collection;

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;

/**
 * Display for a Grid Game.
 *
 */
public class GridGameDisplay implements IGridGameDisplay {
  @SuppressWarnings("unused")
  private IDisplayBridge m_displayBridge;
  private final GridGameFrame m_ui;

  /**
   * Construct a new display for a King's Table game.
   *
   * The display
   *
   * @param ui
   * @see games.strategy.engine.display.IDisplay
   */
  public GridGameDisplay(final GridGameFrame ui) {
    m_ui = ui;
  }

  @Override
  public GridGameFrame getGridGameFrame() {
    return m_ui;
  }

  /**
   * @see games.strategy.engine.display.IDisplay#initialize(games.strategy.engine.display.IDisplayBridge)
   */
  @Override
  public void initialize(final IDisplayBridge bridge) {
    m_displayBridge = bridge;
    // m_displayBridge.toString();
  }

  /**
   * Process a user request to exit the program.
   *
   * @see games.strategy.engine.display.IDisplay#shutdown()
   */
  @Override
  public void shutDown() {
    m_ui.stopGame();
  }

  /**
   * Graphically notify the user of the current game status.
   *
   * @param error
   *        the status message to display
   */
  @Override
  public void setStatus(final String status) {
    m_ui.setStatus(status);
  }

  /**
   * Set the game over status for this display to <code>true</code>.
   */
  @Override
  public void setGameOver()// (CountDownLatch waiting)
  {
    m_ui.setGameOver();// waiting);
  }

  /**
   * Ask the user interface for this display to process a play and zero or more captures.
   *
   * @param territories
   *        <code>Collection</code> of <code>Territory</code>s whose pieces have changed
   */
  @Override
  public void refreshTerritories(final Collection<Territory> territories) {
    m_ui.refreshTerritories(territories);
  }

  @Override
  public void showGridPlayDataMove(final IGridPlayData move) {
    m_ui.showGridPlayDataMove(move);
  }

  @Override
  public void showGridEndTurnData(final IGridEndTurnData endTurnData) {
    m_ui.showGridEndTurnData(endTurnData);
  }

  @Override
  public void initializeGridMapData(final GameMap map) {
    m_ui.initializeGridMapData(map);
  }
}
