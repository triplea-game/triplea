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

import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;

import java.awt.Dimension;
import java.awt.Polygon;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Lane Schwartz (original) and Veqryn (abstraction and major rewrite)
 * @version $LastChangedDate: 2011-11-22 18:21:37 +0800 (Tue, 22 Nov 2011) $
 */
public class GridMapData
{
	// maps String -> Polygons
	protected Map<String, Polygon> m_polys;
	protected int m_gridWidth;
	protected int m_gridHeight;
	protected int m_squareWidth;
	protected int m_squareHeight;
	protected int m_bevelWidth;
	protected int m_bevelHeight;
	
	// protected final GameMap m_map;
	// protected final GameData m_gameData;
	
	public GridMapData(final GameMap map, final int x_dim, final int y_dim, final int squareWidth, final int squareHeight, final int topLeftOffsetWidth, final int topLeftOffsetHeight)
	{
		setMapData(map, x_dim, y_dim, squareWidth, squareHeight, topLeftOffsetWidth, topLeftOffsetHeight);
	}
	
	public synchronized void setMapData(final GameMap map, final int x_dim, final int y_dim, final int squareWidth, final int squareHeight, final int topLeftOffsetWidth, final int topLeftOffsetHeight)
	{
		m_gridWidth = x_dim;
		m_gridHeight = y_dim;
		m_squareWidth = squareWidth;
		m_squareHeight = squareHeight;
		m_bevelWidth = topLeftOffsetWidth;
		m_bevelHeight = topLeftOffsetHeight;
		// m_gameData = gameData;
		// m_map = gameData.getMap();
		int x_offset = m_bevelWidth;
		int y_offset = m_bevelHeight;
		// here we create the polygons for each territory in the grid
		m_polys = new HashMap<String, Polygon>();
		for (int y = 0; y < y_dim; y++)
		{
			for (int x = 0; x < x_dim; x++)
			{
				final Territory territory = map.getTerritoryFromCoordinates(x, y);
				final Polygon p = new Polygon(new int[] { x_offset, x_offset + m_squareWidth, x_offset + m_squareWidth, x_offset },
							new int[] { y_offset, y_offset, y_offset + m_squareHeight, y_offset + m_squareHeight }, 4);
				m_polys.put(territory.getName(), p);
				x_offset += m_squareWidth;
			}
			x_offset = m_bevelWidth;
			y_offset += m_squareHeight;
		}
		initializeGridMapData(map);
	}
	
	public int getBevelWidth()
	{
		return m_bevelWidth;
	}
	
	public int getBevelHeight()
	{
		return m_bevelHeight;
	}
	
	public int getGridWidthNumber()
	{
		return m_gridWidth;
	}
	
	public int getGridHeightNumber()
	{
		return m_gridHeight;
	}
	
	public int getSquareWidth()
	{
		return m_squareWidth;
	}
	
	public int getSquareHeight()
	{
		return m_squareHeight;
	}
	
	public void initializeGridMapData(final GameMap map)
	{
	}
	
	/*
	public GameData getGameData()
	{
		return m_gameData;
	}*/
	
	public synchronized Map<String, Polygon> getStringPolygons()
	{
		return m_polys;
	}
	
	public synchronized Map<Territory, Polygon> getTerritoryPolygons(final GameMap map)
	{
		final Map<Territory, Polygon> polys = new HashMap<Territory, Polygon>();
		for (final Entry<String, Polygon> entry : m_polys.entrySet())
		{
			polys.put(map.getTerritory(entry.getKey()), entry.getValue());
		}
		return polys;
	}
	
	public synchronized Polygon getPolygon(final Territory at)
	{
		return m_polys.get(at.getName());
	}
	
	public synchronized Polygon getPolygon(final String at)
	{
		return m_polys.get(at);
	}
	
	/**
	 * Get the territory at the x,y co-ordinates could be null.
	 */
	public Territory getTerritoryAt(final double x, final double y, final GameMap map)
	{
		final int normal_x = (int) (x - m_bevelWidth);
		final int normal_y = (int) (y - m_bevelHeight);
		if (normal_x < 0 || normal_y < 0 || normal_x > ((m_gridWidth * m_squareWidth) + (m_bevelWidth)) || normal_y > ((m_gridHeight * m_squareHeight) + (m_bevelHeight)))
			return null;
		final int at_x = (normal_x / m_squareWidth);
		final int at_y = (normal_y / m_squareHeight);
		if (at_x < 0 || at_x >= m_gridWidth || at_y < 0 || at_y >= m_gridHeight)
			return null;
		else
			return map.getTerritoryFromCoordinates(at_x, at_y);
	}
	
	public Dimension getMapDimensions()
	{
		return new Dimension(((m_gridWidth * m_squareWidth) + (m_bevelWidth * 2) + 1), ((m_gridHeight * m_squareHeight) + (m_bevelHeight * 2) + 1));
	}
}
