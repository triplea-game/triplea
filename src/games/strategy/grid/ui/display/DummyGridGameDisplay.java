package games.strategy.grid.ui.display;

import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;

import java.util.Collection;

/**
 * 
 * @author veqryn
 * 
 */
public class DummyGridGameDisplay implements IGridGameDisplay
{
	private final MainGameFrame m_ui;
	
	/**
	 * A display which does absolutely nothing
	 */
	public DummyGridGameDisplay()
	{
		m_ui = null;
	}
	
	/**
	 * A display which does absolutely nothing, except for stopping the game on shutdown.
	 * 
	 * @param ui
	 *            MainGameFrame which we will call .stopGame() on if this DummyGridGameDisplay has .shutDown() called.
	 */
	public DummyGridGameDisplay(final MainGameFrame ui)
	{
		m_ui = ui;
	}
	
	public void initialize(final IDisplayBridge bridge)
	{
	}
	
	public void shutDown()
	{
		// make sure to shut down the ui if there is one
		if (m_ui != null)
			m_ui.stopGame();
	}
	
	public void setStatus(final String status)
	{
	}
	
	public void setGameOver()
	{
	}
	
	public void refreshTerritories(final Collection<Territory> territories)
	{
	}
	
	public void showGridPlayDataMove(final IGridPlayData move)
	{
	}
	
	public void showGridEndTurnData(final IGridEndTurnData endTurnData)
	{
	}
	
	public void initializeGridMapData(final GameMap map)
	{
	}
	
	public GridGameFrame getGridGameFrame()
	{
		return null;
	}
}
