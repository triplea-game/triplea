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
package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.*;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.triplea.ui.MapData;
import games.strategy.triplea.util.*;
import games.strategy.ui.Util;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

/**
 * 
 * 
 * 
 * @author Sean Bridges
 */
public class TileManager
{
    private static final Logger s_logger = Logger.getLogger(TileManager.class.getName());
    public final static int TILE_SIZE = 250;
    private List m_tiles = new ArrayList();
    private final Object m_mutex = new Object();
    
    //maps territoryname - collection of drawables
    private final Map m_territoryDrawables = new HashMap();
    //maps territoryname - collection of tiles where the territory is drawn
    private final Map m_territoryTiles = new HashMap();
    
    private Collection m_allUnitDrawables = new ArrayList();
    
    public List getTiles(Rectangle bounds)
    {
        synchronized(m_mutex)
        {
	        List rVal = new ArrayList();
	        Iterator iter = m_tiles.iterator();
	        while (iter.hasNext())
	        {
	            Tile tile = (Tile) iter.next();
	            if (bounds.contains(tile.getBounds()) || tile.getBounds().intersects(bounds))
	                rVal.add(tile);
	        }
	
	        return rVal;
        }
    }

    public Collection getUnitDrawables()
    {
        synchronized(m_mutex)
        {
            return m_allUnitDrawables;
        }
    }
    
    public void createTiles(Rectangle bounds, GameData data, MapData mapData)
    {
        synchronized(m_mutex)
        {
	        //create our tiles
	        m_tiles = new ArrayList();
	        for (int x = 0; (x) * TILE_SIZE < bounds.width; x++)
	        {
	            for (int y = 0; (y) * TILE_SIZE < bounds.height; y++)
	            {
	                m_tiles.add(new Tile(new Rectangle(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE), x,y));
	            }
	        }
        }
     }

    public void resetTiles(GameData data, MapData mapData)
    {
        synchronized(m_mutex)
        {
	        Iterator allTiles = m_tiles.iterator();
	        while (allTiles.hasNext())
	        {
	            Tile tile = (Tile) allTiles.next();
	            tile.clear();
	            
	            int x = tile.getBounds().x / TILE_SIZE;
	            int y = tile.getBounds().y / TILE_SIZE;
	            
	            tile.addDrawable(new BaseMapDrawable(x,y));
	            tile.addDrawable(new ReliefMapDrawable(x,y));
	
	        }

	        Iterator territories = data.getMap().getTerritories().iterator();
	        while (territories.hasNext())
	        {
	            Territory territory = (Territory) territories.next();
	            drawTerritory(territory, data, mapData);
	
	        }
        }

    }
    
    public void updateTerritories(Collection territories, GameData data, MapData mapData)
    {
        synchronized(m_mutex)
        {
	        if(territories == null)
	            return;
	        
	        Iterator iter = territories.iterator();
	        
	        while (iter.hasNext())
	        {
	            Territory territory = (Territory) iter.next();
	            updateTerritory(territory, data, mapData);
	            
	        }
        }
    }

    public void updateTerritory(Territory territory, GameData data, MapData mapData)
    {
        synchronized(m_mutex)
        {
	        s_logger.log(Level.FINER, "Updating " + territory.getName());
	        clearTerritory(territory);
	        drawTerritory(territory, data, mapData);
        }
    }
    
    private void clearTerritory(Territory territory)
    {
        if(m_territoryTiles.get(territory.getName()) == null )
            return;
        Collection drawables = (Collection) m_territoryDrawables.get(territory.getName());
        if(drawables == null || drawables.isEmpty())
            return;
        
        Iterator tiles = ((Collection) m_territoryTiles.get(territory.getName())).iterator();
        while (tiles.hasNext())
        {
            Tile tile = (Tile) tiles.next();
            tile.removeDrawables(drawables);
        }
        
        m_allUnitDrawables.removeAll(drawables);
        
    }

    /**
     * @param data
     * @param mapData
     * @param territory
     */
    private void drawTerritory(Territory territory, GameData data, MapData mapData)
    {
        Set drawnOn = new HashSet();
        Set drawing = new HashSet();
     
        data.acquireChangeLock();
        try
        {
	        Drawable territoryDrawer = null;
	        if(!territory.isWater())
	            territoryDrawer = new LandTerritoryDrawable(territory.getName());
	        
	        drawUnits(territory, data, mapData, drawnOn, drawing);
	        TerritoryNameDrawable nameDrawer = new TerritoryNameDrawable(territory.getName());
	        
	        //add to the relevant tiles
	        Iterator tiles = getTiles(mapData.getBoundingRect(territory.getName())).iterator();
	        while (tiles.hasNext())
	        {
	            Tile tile = (Tile) tiles.next();
	            drawnOn.add(tile);
	            if(territoryDrawer != null)
	            {
	                tile.addDrawable(territoryDrawer);
	                drawing.add(territoryDrawer);
	            }
	            tile.addDrawable(nameDrawer);
	            drawing.add(nameDrawer);
	        }
	        
	        m_territoryDrawables.put(territory.getName(), drawing);
	        m_territoryTiles.put(territory.getName(), drawnOn);
        }
        finally
        {
            data.releaseChangeLock();
        }
    }
    
    private void drawUnits(Territory territory, GameData data, MapData mapData, Set drawnOn, Set drawing)
    {

        Iterator placementPoints = mapData.getPlacementPoints(territory).iterator();
        if (placementPoints == null || !placementPoints.hasNext())
        {
            throw new IllegalStateException("No where to place units:" + territory.getName());
        }

        Point lastPlace = null;

        Iterator unitCategoryIter = UnitSeperator.categorize(territory.getUnits().getUnits()).iterator();

        while (unitCategoryIter.hasNext())
        {
            UnitCategory category = (UnitCategory) unitCategoryIter.next();

            Point place;
            if (placementPoints.hasNext())
            {
                place = (Point) placementPoints.next();
                lastPlace = new Point(place.x, place.y);
            } else
            {
                place = lastPlace;
                lastPlace.x += UnitIconImageFactory.instance().getUnitImageWidth();
            }

            UnitsDrawer drawable = new UnitsDrawer(category.getUnits().size(), category.getType().getName(), category.getOwner().getName(), place,
                    category.getDamaged());
            drawing.add(drawable);
            m_allUnitDrawables.add(drawable);
            
            Iterator tiles = getTiles(
                    new Rectangle(place.x, place.y, UnitIconImageFactory.instance().getUnitImageWidth(), UnitIconImageFactory.instance()
                            .getUnitImageHeight())).iterator();
            while (tiles.hasNext())
            {
                Tile tile = (Tile) tiles.next();
                tile.addDrawable(drawable);
                drawnOn.add(tile);

            }
        }
    }
    
    
    public Image createTerritoryImage(Territory t, GameData data, MapData mapData)
    {
        synchronized(m_mutex)
        {
	        Rectangle bounds = mapData.getBoundingRect(t);
	        
	        Image rVal = Util.createImage( bounds.width, bounds.height, false);
	        Graphics2D graphics = (Graphics2D) rVal.getGraphics();
	        
	        //start as a set to prevent duplicates
	        Set drawablesSet = new HashSet();
	        Iterator tiles = getTiles(bounds).iterator();
	        
	        while(tiles.hasNext())
	        {
	            Tile tile = (Tile) tiles.next();
	            drawablesSet.addAll(tile.getDrawables());
	        }
	        
	        List orderedDrawables = new ArrayList(drawablesSet);
	        Collections.sort(orderedDrawables, new DrawableComparator());
	        Iterator drawers =  orderedDrawables.iterator();
	        while (drawers.hasNext())
	        {
	             Drawable drawer = (Drawable) drawers.next();
	             if(drawer.getLevel() >= Drawable.UNITS_LEVEL)
	                 break;
	             if(drawer.getLevel() == Drawable.TERRITORY_TEXT_LEVEL)
	                 continue;
	             drawer.draw(bounds, data, graphics, mapData);
	        }
	        
	        Iterator iter = mapData.getPolygons(t).iterator();
	        
	        graphics.setStroke(new BasicStroke(5));
	        graphics.setColor(Color.RED);
	        
	        while (iter.hasNext())
            {
                Polygon poly = (Polygon) iter.next();
                poly = new Polygon(poly.xpoints, poly.ypoints, poly.npoints);
                poly.translate(-bounds.x, -bounds.y);
                graphics.drawPolygon(poly);
            }
	        
	        
	        return rVal;
        }

    }

}