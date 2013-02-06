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
package games.puzzle.slidingtiles.ui;

import games.puzzle.slidingtiles.attachments.Tile;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.ui.ImageScrollModel;
import games.strategy.util.Tuple;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * Custom component for displaying a n-puzzle gameboard.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2013-02-03 04:49:39 +0800 (Sun, 03 Feb 2013) $
 */
public class NPuzzleMapPanel extends GridMapPanel
{
	private static final long serialVersionUID = 981372652838512191L;
	private BufferedImage m_backgroundImage = null;
	
	public NPuzzleMapPanel(final GridMapData mapData, final GridGameFrame parentGridGameFrame, final ImageScrollModel imageScrollModel)
	{
		super(mapData, parentGridGameFrame, imageScrollModel);
	}
	
	public void setBackgroundImage(final File file)
	{
		if (file != null)
		{
			try
			{
				final BufferedImage bigimage = ImageIO.read(file);
				final AffineTransform trans = new AffineTransform();
				final double scalex = m_mapData.getMapDimensions().getWidth() / bigimage.getWidth();
				final double scaley = m_mapData.getMapDimensions().getHeight() / bigimage.getHeight();
				trans.scale(scalex, scaley);
				final AffineTransformOp scale = new AffineTransformOp(trans, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
				m_backgroundImage = new BufferedImage(((int) m_mapData.getMapDimensions().getWidth()), ((int) m_mapData.getMapDimensions().getHeight()), bigimage.getType());
				scale.filter(bigimage, m_backgroundImage);
			} catch (final IOException e)
			{
				m_backgroundImage = null;
			}
		}
		else
		{
			m_backgroundImage = null;
		}
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
	 * Draw the current map and pieces.
	 */
	@Override
	protected void paintComponentMiddleLayer(final Graphics2D g2d, final int topLeftX, final int topLeftY)
	{
		final NPuzzleMapData nPuzzleMapData = (NPuzzleMapData) m_mapData;
		final Dimension mapDimension = nPuzzleMapData.getMapDimensions();
		g2d.setColor(Color.lightGray);
		g2d.fillRect(0, 0, mapDimension.width, mapDimension.height);
		g2d.setColor(Color.white);
		g2d.fillRect(m_mapData.getTopLeftOffsetWidth(), m_mapData.getTopLeftOffsetHeight(), getWidth() - (m_mapData.getTopLeftOffsetWidth() * 2), getHeight()
					- (m_mapData.getTopLeftOffsetHeight() * 2));
		// g.fillRect(0, 0, getWidth(), getHeight());
		for (final Map.Entry<Territory, Polygon> entry : nPuzzleMapData.getPolygons().entrySet())
		{
			final Polygon p = entry.getValue();
			final Territory at = entry.getKey();
			final Tile tile = (Tile) at.getAttachment("tile");
			if (tile != null)
			{
				final int value = tile.getValue();
				if (value != 0)
				{
					final Rectangle square = p.getBounds();
					final Rectangle tileData = nPuzzleMapData.getLocation(value);
					if (m_backgroundImage == null)
					{
						g2d.setColor(Color.black);
						g2d.drawString(Integer.toString(value), square.x + (square.width * 5 / 12), square.y + (square.height * 7 / 12));
					}
					else if (tileData != null)
					{
						g2d.drawImage(m_backgroundImage, square.x, square.y, square.x + square.width, square.y + square.height, tileData.x, tileData.y, tileData.x + tileData.width, tileData.y
									+ tileData.height,
									this);
					}
					else
					{
						g2d.setColor(Color.white);
						g2d.fillRect(square.x, square.y, square.width, square.height);
					}
				}
			}
			g2d.setColor(Color.black);
			g2d.drawPolygon(p);
		}
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
		if (m_clickedAt == null || m_releasedAt == null)
		{
			// If either m_clickedAt==null or m_releasedAt==null,
			// the play is invalid and must have been interrupted.
			// So, reset the member variables, and throw an exception.
			m_clickedAt = null;
			m_releasedAt = null;
			throw new InterruptedException("Interrupted while waiting for play.");
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
	
	@Override
	public void setMouseShadowUnits(final Collection<Unit> units)
	{
	}
	
	@Override
	protected String isValidPlay(final IGridPlayData play)
	{
		return null;
	}
	
	@Override
	protected Collection<Territory> getCapturesForPlay(final IGridPlayData play)
	{
		return null;
	}
	
	@Override
	protected Tuple<Collection<Territory>, Collection<Territory>> getValidMovesList(final Territory clickedOn)
	{
		return null;
	}
}
