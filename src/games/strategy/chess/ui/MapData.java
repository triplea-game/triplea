package games.strategy.chess.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;

import java.awt.Dimension;
import java.awt.Polygon;
import java.util.HashMap;
import java.util.Map;

public class MapData
{
	// maps String -> Polygons
	private final Map<Territory, Polygon> m_polys;
	private final int m_gridWidth;
	private final int m_gridHeight;
	private final int m_squareWidth;
	private final int m_squareHeight;
	private final int m_topLeftOffsetWidth;
	private final int m_topLeftOffsetHeight;
	private final GameMap m_map;
	private final GameData m_gameData;
	
	public MapData(final GameData gameData, final int x_dim, final int y_dim)
	{
		m_polys = new HashMap<Territory, Polygon>();
		m_gridWidth = x_dim;
		m_gridHeight = y_dim;
		m_squareWidth = 50;
		m_squareHeight = 50;
		m_topLeftOffsetWidth = 50;
		m_topLeftOffsetHeight = 50;
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
	}
	
	GameData getGameData()
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
		final int at_x = (int) ((x - m_topLeftOffsetWidth) / m_squareWidth);
		final int at_y = (int) ((y - m_topLeftOffsetHeight) / m_squareHeight);
		if (at_x < m_topLeftOffsetWidth || at_x >= m_gridWidth || at_y < m_topLeftOffsetHeight || at_y >= m_gridHeight)
			return null;
		else
			return m_map.getTerritoryFromCoordinates(at_x, at_y);
	}
	
	public Dimension getMapDimensions()
	{
		return new Dimension(m_gridWidth * m_squareWidth + m_topLeftOffsetWidth * 2 + 1, m_gridHeight * m_squareHeight + m_topLeftOffsetHeight * 2 + 1);
	}
}
