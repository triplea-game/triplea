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
package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.MapData;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.ImageScrollerSmallView;
import games.strategy.ui.Util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * 
 * 
 * @author Sean Bridges
 */
public class SmallMapImageManager
{
	private static final Logger s_logger = Logger.getLogger(SmallMapImageManager.class.getName());
	private final int UNIT_BOX_SIZE = 4;
	private final ImageScrollerSmallView m_view;
	private final Image m_offscreen;
	private final TileManager m_tileManager;
	
	public SmallMapImageManager(final ImageScrollerSmallView view, final BufferedImage offscreen, final TileManager tileManager)
	{
		m_view = view;
		m_offscreen = Util.copyImage(offscreen, false);
		m_tileManager = tileManager;
	}
	
	public void update(final GameData data, final MapData mapData)
	{
		final Stopwatch stopwatch = new Stopwatch(s_logger, Level.FINEST, "Small map updating took");
		final Graphics onScreenGraphics = m_view.getOffScreenImage().getGraphics();
		onScreenGraphics.drawImage(m_offscreen, 0, 0, null);
		final Iterator<UnitsDrawer> iter = new ArrayList<UnitsDrawer>(m_tileManager.getUnitDrawables()).iterator();
		while (iter.hasNext())
		{
			final UnitsDrawer drawer = iter.next();
			final int x = (int) (drawer.getPlacementPoint().x * m_view.getRatioX());
			final int y = (int) (drawer.getPlacementPoint().y * m_view.getRatioY());
			onScreenGraphics.setColor(mapData.getPlayerColor(drawer.getPlayer()).darker());
			onScreenGraphics.fillRect(x, y, UNIT_BOX_SIZE, UNIT_BOX_SIZE);
		}
		onScreenGraphics.dispose();
		stopwatch.done();
	}
	
	public void updateTerritoryOwner(final Territory t, final GameData data, final MapData mapData)
	{
		if (t.isWater())
			return;
		final Rectangle bounds = new Rectangle(mapData.getBoundingRect(t.getName()));
		// create a large image for the territory
		final Image largeImage = Util.createImage(bounds.width, bounds.height, true);
		// make it transparent
		// http://www-106.ibm.com/developerworks/library/j-begjava/
		{
			final Graphics2D g = (Graphics2D) largeImage.getGraphics();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
			g.setColor(new Color(0));
			g.fillRect(0, 0, bounds.width, bounds.height);
			g.dispose();
		}
		// draw the territory
		{
			final Graphics g = largeImage.getGraphics();
			final LandTerritoryDrawable drawable = new LandTerritoryDrawable(t.getName());
			drawable.draw(bounds, data, (Graphics2D) g, mapData, null, null);
			g.dispose();
		}
		// scale it down
		int thumbWidth = (int) (bounds.width * m_view.getRatioX());
		int thumbHeight = (int) (bounds.height * m_view.getRatioY());
		// make the image a little bigger
		// the images wont overlap perfectly after being scaled, make them a little bigger to rebalance that
		thumbWidth += 3;
		thumbHeight += 3;
		final int thumbsX = (int) (bounds.x * m_view.getRatioX()) - 1;
		final int thumbsY = (int) (bounds.y * m_view.getRatioY()) - 1;
		// create the thumb image
		final Image thumbImage = Util.createImage(thumbWidth, thumbHeight, true);
		{
			final Graphics g = thumbImage.getGraphics();
			g.drawImage(largeImage, 0, 0, thumbImage.getWidth(null), thumbImage.getHeight(null), null);
			g.dispose();
		}
		{
			final Graphics g = m_offscreen.getGraphics();
			// draw it on our offscreen
			g.drawImage(thumbImage, thumbsX, thumbsY, thumbImage.getWidth(null), thumbImage.getHeight(null), null);
			g.dispose();
		}
	}
}
