/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.kingstable.ui.display;

import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.grid.ui.display.IGridGameDisplay;

import java.util.Collection;

/**
 * Dummy display for a King's Table game, for use in testing.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class DummyDisplay implements IGridGameDisplay
{
	/**
	 * @see games.strategy.engine.display.IKingsTableDisplay#performPlay(Territory,Territory,Collection<Territory>)
	 */
	public void refreshTerritories(final Collection<Territory> territories)
	{
	}
	
	/**
	 * @see games.strategy.grid.ui.display.IGridGameDisplay#setGameOver()
	 */
	public void setGameOver()// CountDownLatch waiting) {
	{
	}
	
	/**
	 * @see games.strategy.grid.ui.display.IGridGameDisplay#setStatus(String)
	 */
	public void setStatus(final String status)
	{
	}
	
	/**
	 * @see games.strategy.grid.ui.display.IGridGameDisplay#initialize(IDisplayBridge)
	 */
	public void initialize(final IDisplayBridge bridge)
	{
	}
	
	/**
	 * @see games.strategy.grid.ui.display.IGridGameDisplay#shutDown()
	 */
	public void shutDown()
	{
	}
	
	public void initializeGridMapData()
	{
	}
	
	public GridGameFrame getGridGameFrame()
	{
		return null;
	}
	
	public void showGridPlayDataMove(final IGridPlayData move)
	{
	}
}
