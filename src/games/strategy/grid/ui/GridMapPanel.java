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
package games.strategy.grid.ui;

import games.strategy.common.image.UnitImageFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Custom component for displaying a King's Table gameboard and pieces.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2012-04-19 18:13:58 +0800 (Thu, 19 Apr 2012) $
 */
public abstract class GridMapPanel extends JComponent implements MouseListener
{
	private static final long serialVersionUID = -1318170171400997968L;
	protected final GridMapData m_mapData;
	protected final GameData m_gameData;
	protected Territory m_clickedAt = null;
	protected Territory m_releasedAt = null;
	protected final Map<Territory, Image> m_images;
	protected CountDownLatch m_waiting;
	protected final UnitImageFactory m_imageFactory;
	
	public GridMapPanel(final GridMapData mapData)
	{
		m_waiting = null;
		m_mapData = mapData;
		m_gameData = m_mapData.getGameData();
		final String mapName = (String) m_gameData.getProperties().get(Constants.MAP_NAME);
		if (mapName == null || mapName.trim().length() == 0)
		{
			throw new IllegalStateException("Map name property not set on game");
		}
		m_imageFactory = new UnitImageFactory();
		m_imageFactory.setResourceLoader(ResourceLoader.getMapResourceLoader(mapName, true));
		final Dimension mapDimension = m_mapData.getMapDimensions();
		this.setMinimumSize(mapDimension);
		this.setPreferredSize(mapDimension);
		this.setSize(mapDimension);
		m_images = new HashMap<Territory, Image>();
		for (final Territory at : m_mapData.getPolygons().keySet())
		{
			updateImage(at);
		}
		this.addMouseListener(this);
		this.setOpaque(true);
	}
	
	/**
	 * Get the size of the map.
	 * 
	 * @return the size of the map
	 */
	public Dimension getMapDimensions()
	{
		return m_mapData.getMapDimensions();
	}
	
	/**
	 * Update the user interface based on a game play.
	 * 
	 * @param start
	 *            <code>Territory</code> where the moving piece began
	 * @param end
	 *            <code>Territory</code> where the moving piece ended
	 * @param captured
	 *            <code>Collection</code> of <code>Territory</code>s whose pieces were captured during the play
	 */
	protected void performPlay(final Territory start, final Territory end, final Collection<Territory> captured)
	{
		updateImage(start);
		updateImage(end);
		for (final Territory at : captured)
			updateImage(at);
		// Ask Swing to repaint this panel when it's convenient
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				repaint();
			}
		});
	}
	
	/**
	 * Update the image for a <code>Territory</code> based on the contents of that <code>Territory</code>.
	 * 
	 * @param at
	 *            the <code>Territory</code> to update
	 */
	private void updateImage(final Territory at)
	{
		if (at != null)
		{
			if (at.getUnits().size() == 1)
			{
				// Get image for exactly one unit
				final Unit u = (Unit) at.getUnits().getUnits().toArray()[0];
				final Image image = m_imageFactory.getImage(u.getType(), u.getOwner(), m_gameData);
				m_images.put(at, image);
			}
			else
			{
				m_images.remove(at);
			}
		}
	}
	
	@Override
	protected abstract void paintComponent(final Graphics g);
	
	public void mouseClicked(final MouseEvent e)
	{
	}
	
	public void mouseEntered(final MouseEvent e)
	{
	}
	
	public void mouseExited(final MouseEvent e)
	{
	}
	
	/**
	 * Process the mouse button being pressed.
	 */
	public void mousePressed(final MouseEvent e)
	{
		// After this method has been called,
		// the Territory corresponding to the cursor location when the mouse was pressed
		// will be stored in the private member variable m_clickedAt.
		m_clickedAt = m_mapData.getTerritoryAt(e.getX(), e.getY());
	}
	
	/**
	 * Process the mouse button being released.
	 */
	public void mouseReleased(final MouseEvent e)
	{
		// After this method has been called,
		// the Territory corresponding to the cursor location when the mouse was released
		// will be stored in the private member variable m_releasedAt.
		m_releasedAt = m_mapData.getTerritoryAt(e.getX(), e.getY());
		// The waitForPlay method is waiting for mouse input.
		// Let it know that we have processed mouse input.
		if (m_waiting != null)
			m_waiting.countDown();
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
	public GridPlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge, final CountDownLatch waiting) throws InterruptedException
	{
		// Make sure we have a valid CountDownLatch.
		if (waiting == null || waiting.getCount() != 1)
			throw new IllegalArgumentException("CountDownLatch must be non-null and have getCount()==1");
		// The mouse listeners need access to the CountDownLatch, so store as a member variable.
		m_waiting = waiting;
		// Wait for a play or an attempt to leave the game
		m_waiting.await();
		if (m_clickedAt == null || m_releasedAt == null)
		{
			// If either m_clickedAt==null or m_releasedAt==null,
			// the play is invalid and must have been interrupted.
			// So, reset the member variables, and throw an exception.
			m_clickedAt = null;
			m_releasedAt = null;
			throw new InterruptedException("Interrupted while waiting for play.");
		}
		else if (m_clickedAt == m_releasedAt)
		{
			// If m_clickedAt==m_releasedAt,
			// the play started and stopped on the same Territory.
			// This is a blatantly invalid play, but we can't reset the CountDownLatch here,
			// so reset the member variables and return null.
			m_clickedAt = null;
			m_releasedAt = null;
			return null;
		}
		else
		{
			// We have a valid play!
			// Reset the member variables, and return the play.
			final GridPlayData play = new GridPlayData(m_clickedAt, m_releasedAt);
			m_clickedAt = null;
			m_releasedAt = null;
			return play;
		}
	}
}
