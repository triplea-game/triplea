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
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.image.UnitImageFactory;
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
    public final static int TILE_SIZE = 256;
    private List<Tile> m_tiles = new ArrayList<Tile>();
    private final Object m_mutex = new Object();
    
    //maps territoryname - collection of drawables
    private final Map<String, Set<IDrawable>> m_territoryDrawables = new HashMap<String, Set<IDrawable>>();
    //maps territoryname - collection of tiles where the territory is drawn
    private final Map<String, Set<Tile>> m_territoryTiles = new HashMap<String, Set<Tile>>();
    
    private Collection<UnitsDrawer> m_allUnitDrawables = new ArrayList<UnitsDrawer>();
    
    public List<Tile> getTiles(Rectangle bounds)
    {
        synchronized(m_mutex)
        {
	        List<Tile> rVal = new ArrayList<Tile>();
	        Iterator<Tile> iter = m_tiles.iterator();
	        while (iter.hasNext())
	        {
	            Tile tile = iter.next();
	            if (bounds.contains(tile.getBounds()) || tile.getBounds().intersects(bounds))
	                rVal.add(tile);
	        }
	
	        return rVal;
        }
    }

    public Collection<UnitsDrawer> getUnitDrawables()
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
	        m_tiles = new ArrayList<Tile>();
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
        data.acquireChangeLock();
        
        try
        {
        
        
	        synchronized(m_mutex)
	        {
		        Iterator<Tile> allTiles = m_tiles.iterator();
		        while (allTiles.hasNext())
		        {
		            Tile tile = allTiles.next();
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
        finally
        {
            data.releaseChangeLock();
        }

    }
    
    public void updateTerritories(Collection territories, GameData data, MapData mapData)
    {
        data.acquireChangeLock();
        try
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
        finally
        {
            data.releaseChangeLock();
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
        Collection drawables = m_territoryDrawables.get(territory.getName());
        if(drawables == null || drawables.isEmpty())
            return;
        
        Iterator tiles = m_territoryTiles.get(territory.getName()).iterator();
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
        Set<Tile> drawnOn = new HashSet<Tile>();
        Set<IDrawable> drawing = new HashSet<IDrawable>();
        

        drawUnits(territory, data, mapData, drawnOn, drawing);
        
        if(!territory.isWater())
            drawing.add(new LandTerritoryDrawable(territory.getName()));
        
        drawing.add(new TerritoryNameDrawable(territory.getName()));
        
        TerritoryAttatchment ta = TerritoryAttatchment.get(territory);
        if(ta != null &&  ta.isCapital() && mapData.drawCapitolMarkers())
        {
            PlayerID capitalOf = data.getPlayerList().getPlayerID(ta.getCapital());
            drawing.add(new CapitolMarkerDrawable(capitalOf, territory));
        }
        
        if(ta != null && ta.isVictoryCity())
        {
            drawing.add(new VCDrawable(territory));
        }
        
        //add to the relevant tiles
        Iterator<Tile> tiles = getTiles(mapData.getBoundingRect(territory.getName())).iterator();
        while (tiles.hasNext())
        {
            Tile tile = tiles.next();
            drawnOn.add(tile);
            tile.addDrawbles(drawing);
        }
        
        m_territoryDrawables.put(territory.getName(), drawing);
        m_territoryTiles.put(territory.getName(), drawnOn);
   
    }
    
    private void drawUnits(Territory territory, GameData data, MapData mapData, Set<Tile> drawnOn, Set<IDrawable> drawing)
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

            
            boolean overflow;
            if (placementPoints.hasNext())
            {
                lastPlace = new Point( (Point) placementPoints.next());
                overflow = false;
            } else
            {
                lastPlace = new Point(lastPlace);
                lastPlace.x += UnitImageFactory.instance().getUnitImageWidth();
                overflow = true;
            }

            UnitsDrawer drawable = new UnitsDrawer(category.getUnits().size(), category.getType().getName(), category.getOwner().getName(), lastPlace,
                    category.getDamaged(), overflow, territory.getName());
            drawing.add(drawable);
            m_allUnitDrawables.add(drawable);
            
            Iterator<Tile> tiles = getTiles(
                    new Rectangle(lastPlace.x, lastPlace.y, UnitImageFactory.instance().getUnitImageWidth(), UnitImageFactory.instance()
                            .getUnitImageHeight())).iterator();
            while (tiles.hasNext())
            {
                Tile tile = tiles.next();
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
	        Set<IDrawable> drawablesSet = new HashSet<IDrawable>();
	        Iterator<Tile> tiles = getTiles(bounds).iterator();
	        
	        while(tiles.hasNext())
	        {
	            Tile tile = tiles.next();
	            drawablesSet.addAll(tile.getDrawables());
	        }
	        
	        List<IDrawable> orderedDrawables = new ArrayList<IDrawable>(drawablesSet);
	        Collections.sort(orderedDrawables, new DrawableComparator());
	        Iterator<IDrawable> drawers =  orderedDrawables.iterator();
	        while (drawers.hasNext())
	        {
	             IDrawable drawer = drawers.next();
	             if(drawer.getLevel() >= IDrawable.UNITS_LEVEL)
	                 break;
	             if(drawer.getLevel() == IDrawable.TERRITORY_TEXT_LEVEL)
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
	        
	        graphics.dispose();
	        return rVal;
        }

    }
    
    
    public List<Unit> getUnitsAtPoint(int x, int y, GameData gameData)
    {
        for(UnitsDrawer drawer : m_allUnitDrawables)
        {
            Point placementPoint = drawer.getPlacementPoint();
            if(x > placementPoint.x && x < placementPoint.x + UnitImageFactory.UNIT_ICON_WIDTH)
            {
                if(y > placementPoint.y && y < placementPoint.y + UnitImageFactory.UNIT_ICON_HEIGHT)
                {
                    return drawer.getUnits(gameData);
                }
            }
        }
        return Collections.emptyList();
    }

    
}