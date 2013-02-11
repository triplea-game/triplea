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
package games.puzzle.slidingtiles.delegate;

import games.puzzle.slidingtiles.attachments.Tile;
import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.ui.display.IGridGameDisplay;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Responsible for initializing an N-Puzzle game.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class InitializationDelegate extends AbstractDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		final GameData data = getData();
		final GameMap map = data.getMap();
		final int width = data.getProperties().get("Width", map.getXDimension());
		final int height = data.getProperties().get("Height", map.getYDimension());
		if (width != map.getXDimension() || height != map.getYDimension())
		{
			m_bridge.getHistoryWriter().startEvent("Changing Map Dimensions");
			final Territory t1 = map.getTerritories().iterator().next();
			final String name = t1.getName().substring(0, t1.getName().indexOf("_"));
			m_bridge.addChange(ChangeFactory.addGridGameMapChange(map, "square", name, width, height, new HashSet<String>(), "implicit", "implicit", "explicit"));
		}
		final Territory[][] board = new Territory[width][height];
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus("Shuffling tiles...");
		m_bridge.getHistoryWriter().startEvent("Initializing board");
		final CompositeChange initializingBoard = new CompositeChange();
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				board[x][y] = map.getTerritoryFromCoordinates(x, y);
				final Tile tile = new Tile(x + y * width);
				// System.out.println("board["+x+"]["+y+"]=="+(x + y*width));
				final Change change = ChangeFactory.addAttachmentChange(tile, board[x][y], "tile");
				initializingBoard.add(change);
			}
		}
		m_bridge.addChange(initializingBoard);
		display.initializeGridMapData(map);
		display.refreshTerritories(null);
		m_bridge.getHistoryWriter().startEvent("Randomizing board");
		// CompositeChange randomizingBoard = new CompositeChange();
		Territory blank = board[0][0];
		Territory dontChooseNextTime = null;
		Territory swap = null;
		// System.out.println("Random stuff!");
		final int numberOfShuffles = getData().getProperties().get("Difficulty Level", 7);
		// int numberOfShuffles = 0;
		// Randomly shuffle the tiles on the board,
		// but don't move a tile back to where it just was.
		final Random random = new Random();
		for (int i = 0; i < numberOfShuffles; i++)
		{
			while (swap == null || swap.equals(dontChooseNextTime))
			{
				final List<Territory> neighbors = new ArrayList<Territory>(map.getNeighbors(blank));
				swap = neighbors.get(random.nextInt(neighbors.size()));
			}
			try
			{
				Thread.sleep(75);
			} catch (final InterruptedException e)
			{
			}
			PlayDelegate.swap(m_bridge, swap, blank);
			// randomizingBoard.add(change);
			dontChooseNextTime = blank;
			blank = swap;
			swap = null;
		}
		display.setStatus(" ");
		// m_bridge.addChange(randomizingBoard);
		// display.performPlay();
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final SlidingTilesInitializationExtendedDelegateState state = new SlidingTilesInitializationExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final SlidingTilesInitializationExtendedDelegateState s = (SlidingTilesInitializationExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	public boolean stuffToDoInThisDelegate()
	{
		return false;
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class does not implement the IRemote interface, so return null.
		return null;
	}
}


class SlidingTilesInitializationExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -6612138982624223426L;
	Serializable superState;
	// add other variables here:
}
