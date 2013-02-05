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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;

import java.awt.Dimension;
import java.awt.Polygon;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate: 2011-11-22 18:21:37 +0800 (Tue, 22 Nov 2011) $
 */
public class GridMapData
{
	// maps String -> Polygons
	protected final Map<Territory, Polygon> m_polys = new HashMap<Territory, Polygon>();
	protected final int m_gridWidth;
	protected final int m_gridHeight;
	protected final int m_squareWidth;
	protected final int m_squareHeight;
	protected final int m_topLeftOffsetWidth;
	protected final int m_topLeftOffsetHeight;
	protected final GameMap m_map;
	protected final GameData m_gameData;
	
	public GridMapData(final GameData gameData, final int x_dim, final int y_dim, final int squareWidth, final int squareHeight, final int topLeftOffsetWidth, final int topLeftOffsetHeight)
	{
		m_gridWidth = x_dim;
		m_gridHeight = y_dim;
		m_squareWidth = squareWidth;
		m_squareHeight = squareHeight;
		m_topLeftOffsetWidth = topLeftOffsetWidth;
		m_topLeftOffsetHeight = topLeftOffsetHeight;
		m_gameData = gameData;
		m_map = gameData.getMap();
		int x_offset = m_topLeftOffsetWidth;
		int y_offset = m_topLeftOffsetHeight;
		// here we create the polygons for each territory in the grid
		for (int y = 0; y < y_dim; y++)
		{
			for (int x = 0; x < x_dim; x++)
			{
				final Territory territory = m_map.getTerritoryFromCoordinates(x, y);
				m_polys.put(territory, new Polygon(new int[] { x_offset, x_offset + m_squareWidth, x_offset + m_squareWidth, x_offset },
													new int[] { y_offset, y_offset, y_offset + m_squareHeight, y_offset + m_squareHeight }, 4));
				x_offset += m_squareWidth;
			}
			x_offset = m_topLeftOffsetWidth;
			y_offset += m_squareHeight;
		}
		initializeGridMapData();
	}
	
	public int getTopLeftOffsetWidth()
	{
		return m_topLeftOffsetWidth;
	}
	
	public int getTopLeftOffsetHeight()
	{
		return m_topLeftOffsetHeight;
	}
	
	public void initializeGridMapData()
	{
	}
	
	public GameData getGameData()
	{
		return m_gameData;
	}
	
	public Map<Territory, Polygon> getPolygons()
	{
		return m_polys;
	}
	
	public Polygon getPolygon(final Territory at)
	{
		return m_polys.get(at);
	}
	
	/**
	 * Get the territory at the x,y co-ordinates could be null.
	 */
	public Territory getTerritoryAt(final double x, final double y)
	{
		final int normal_x = (int) (x - m_topLeftOffsetWidth);
		final int normal_y = (int) (y - m_topLeftOffsetHeight);
		if (normal_x < 0 || normal_y < 0 || normal_x > ((m_gridWidth * m_squareWidth) + (m_topLeftOffsetWidth)) || normal_y > ((m_gridHeight * m_squareHeight) + (m_topLeftOffsetHeight)))
			return null;
		final int at_x = (normal_x / m_squareWidth);
		final int at_y = (normal_y / m_squareHeight);
		if (at_x < 0 || at_x >= m_gridWidth || at_y < 0 || at_y >= m_gridHeight)
			return null;
		else
			return m_map.getTerritoryFromCoordinates(at_x, at_y);
	}
	
	public Dimension getMapDimensions()
	{
		return new Dimension(((m_gridWidth * m_squareWidth) + (m_topLeftOffsetWidth * 2) + 1), ((m_gridHeight * m_squareHeight) + (m_topLeftOffsetHeight * 2) + 1));
	}
}
