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
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;
import games.strategy.grid.ui.GridMapData;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lane Schwartz (original) and Veqryn (abstraction)
 * @version $LastChangedDate: 2011-11-22 18:21:37 +0800 (Tue, 22 Nov 2011) $
 */
public class NPuzzleMapData extends GridMapData
{
	protected volatile Map<Integer, Rectangle> m_rects;
	
	public NPuzzleMapData(final GameMap map, final int x_dim, final int y_dim, final int squareWidth, final int squareHeight, final int topLeftOffsetWidth, final int topLeftOffsetHeight)
	{
		super(map, x_dim, y_dim, squareWidth, squareHeight, topLeftOffsetWidth, topLeftOffsetHeight);
	}
	
	@Override
	public void initializeGridMapData(final GameMap map)
	{
		m_rects = new HashMap<Integer, Rectangle>();
		for (final Territory territory : map.getTerritories())
		{
			final Tile tile = (Tile) territory.getAttachment("tile");
			if (tile != null)
			{
				final int value = tile.getValue();
				if (value != 0)
				{
					final int tileX = value % m_gridWidth;
					final int tileY = value / m_gridWidth;
					final Rectangle rectangle = new Rectangle(tileX * m_squareWidth, tileY * m_squareHeight, m_squareWidth, m_squareHeight);
					m_rects.put(value, rectangle);
				}
			}
		}
	}
	
	public Rectangle getLocation(final int value)
	{
		return m_rects.get(value);
	}
}
