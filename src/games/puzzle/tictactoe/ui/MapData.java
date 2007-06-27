/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.puzzle.tictactoe.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.Territory;

import java.awt.Dimension;
import java.awt.Polygon;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class MapData
{
    //maps String -> Polygons
    private Map<Territory, Polygon> m_polys;
    
    private int m_gridWidth;
    private int m_gridHeight;
    private int m_squareWidth; 
    private int m_squareHeight;
    
    private GameMap m_map;
    
    private GameData m_gameData;
    
    public MapData(GameData gameData, int x_dim, int y_dim, int squareWidth, int squareHeight) 
    {
        m_polys = new HashMap<Territory, Polygon>();

        m_gridWidth = x_dim;
        m_gridHeight = y_dim;
        m_squareWidth = squareWidth;
        m_squareHeight = squareHeight;
        m_gameData = gameData;
        m_map = gameData.getMap();
        
        int x_offset = 0;
        int y_offset = 0;
        
        for (int y = 0; y < y_dim; y++)
        {
            for (int x = 0; x < x_dim; x++)
            {
                //String squareName = mapName + "_" + x + "_" + y;
                Territory territory = m_map.getTerritoryFromCoordinates(x,y);
 
                m_polys.put(
                        territory,
                        new Polygon(
                                new int[]{x_offset, x_offset+squareWidth, x_offset+squareWidth, x_offset},
                                new int[]{y_offset, y_offset, y_offset+squareHeight, y_offset+squareHeight}, 
                                4)
                );

                x_offset += squareWidth;
            }
            
            x_offset = 0;
            y_offset += squareHeight;
            
        }
                
    }
    
    GameData getGameData() 
    {
        return m_gameData;
    }
    
    public Map<Territory,Polygon> getPolygons()
    {
        return m_polys;
    }
    
    public Polygon getPolygon(Territory at)
    {
        return m_polys.get(at);
    }
    
    /**
     * Get the territory at the x,y co-ordinates could be null.
     */
    public Territory getTerritoryAt(double x, double y)
    {
        int at_x = (int) (x / m_squareWidth);
        int at_y = (int) (y / m_squareHeight);
        
        if (at_x < 0 || at_x >= m_gridWidth || at_y < 0 || at_y >= m_gridHeight)
            return null;
        else
            return m_map.getTerritoryFromCoordinates(at_x, at_y);
    }

    public Dimension getMapDimensions()
    {
        return new Dimension(m_gridWidth*m_squareWidth+1, m_gridHeight*m_squareHeight+1);
    }
}
