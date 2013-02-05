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
package games.puzzle.tictactoe.ui;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;
import games.strategy.grid.ui.GridPlayData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Custom component for displaying a Tic Tac Toe gameboard and pieces.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2013-02-03 04:49:39 +0800 (Sun, 03 Feb 2013) $
 */
public class TicTacToeMapPanel extends GridMapPanel implements MouseListener
{
	private static final long serialVersionUID = 96734493518077373L;
	
	public TicTacToeMapPanel(final GridMapData mapData, final GridGameFrame parentGridGameFrame)
	{
		super(mapData, parentGridGameFrame);
		updateAllImages();
		m_gameData.addDataChangeListener(new GameDataChangeListener()
		{
			public void gameDataChanged(final Change change)
			{
				updateAllImages();
			}
		});
	}
	
	/**
	 * Draw the current map and pieces.
	 */
	@Override
	protected void paintComponentMiddleLayer(final Graphics2D g)
	{
		g.setColor(Color.white);
		g.fillRect(0, 0, getWidth(), getHeight());
		for (final Map.Entry<Territory, Polygon> entry : m_mapData.getPolygons().entrySet())
		{
			final Polygon p = entry.getValue();
			final Territory at = entry.getKey();
			final Color backgroundColor = Color.WHITE;
			g.setColor(Color.black);
			final Image image = m_images.get(at);
			if (image != null)
			{
				final Rectangle square = p.getBounds();
				g.drawImage(image, square.x, square.y, square.width, square.height, backgroundColor, null);
			}
			g.drawPolygon(p);
		}
	}
	
	/**
	 * Process the mouse button being pressed.
	 */
	@Override
	public void mousePressed(final MouseEvent e)
	{
		// After this method has been called,
		// the Territory corresponding to the cursor location when the mouse was pressed
		// will be stored in the private member variable m_clickedAt.
		m_clickedAt = m_mapData.getTerritoryAt(e.getX(), e.getY());
		// The waitForPlay method is waiting for mouse input.
		// Let it know that we have processed mouse input.
		if (m_waiting != null)
			m_waiting.countDown();
	}
	
	/**
	 * Process the mouse button being released.
	 */
	@Override
	public void mouseReleased(final MouseEvent e)
	{
	}
	
	@Override
	protected MouseMotionListener getMouseMotionListener()
	{
		return new MouseMotionAdapter()
		{
		};
	}
	
	/**
	 * Wait for a player to play.
	 * 
	 * @param player
	 *            the player to wait on
	 * @param bridge
	 *            the bridge for player
	 * @param waiting
	 *            a <code>CountDownLatch</code> used to wait for user input - must be non-null and have and have <code>getCount()==1</code>
	 * @return PlayData representing a play, or <code>null</code> if the play started and stopped on the same <code>Territory</code>
	 * @throws InterruptedException
	 *             if the play was interrupted
	 */
	@Override
	public GridPlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting) throws InterruptedException
	{
		// Make sure we have a valid CountDownLatch.
		if (waiting == null || waiting.getCount() != 1)
			throw new IllegalArgumentException("CountDownLatch must be non-null and have getCount()==1");
		// The mouse listeners need access to the CountDownLatch, so store as a member variable.
		m_waiting = waiting;
		// Wait for a play or an attempt to leave the game
		m_waiting.await();
		if (m_clickedAt == null)
		{
			// If m_clickedAt==null,
			// the play is invalid and must have been interrupted.
			// So, reset the member variable, and throw an exception.
			m_clickedAt = null;
			throw new InterruptedException("Interrupted while waiting for play.");
		}
		else
		{
			// We have a valid play!
			// Reset the member variables, and return the play.
			final Territory play = m_clickedAt;
			m_clickedAt = null;
			return new GridPlayData(play, null);
		}
	}
	
	@Override
	protected String isValidPlay(final GridPlayData play)
	{
		return null;
	}
	
	@Override
	protected Collection<Territory> getValidMovesList(final Territory clickedOn)
	{
		return null;
	}
}
