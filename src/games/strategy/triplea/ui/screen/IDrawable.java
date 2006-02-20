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
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.image.*;
import games.strategy.triplea.ui.*;
import games.strategy.triplea.util.Stopwatch;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

/**
 * @author Sean Bridges
 */
public interface IDrawable
{
    public Logger s_logger = Logger.getLogger(IDrawable.class.getName());
    
    public static final int BASE_MAP_LEVEL = 1;
    public static final int POLYGONS_LEVEL = 2;
    public static final int RELIEF_LEVEL = 3;
    
    public static final int CAPITOL_MARKER_LEVEL = 7;
    public static final int VC_MARKER_LEVEL = 8;
    
    public static final int TERRITORY_TEXT_LEVEL = 10;
    public static final int UNITS_LEVEL = 11;
    
    public static final int TERRITORY_OVERLAY_LEVEL = 12;
    
    /**
     * Draw the tile
     */
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData);

    public int getLevel();
}

class DrawableComparator implements Comparator<IDrawable>
{

    public int compare(IDrawable o1, IDrawable o2)
    {
        return o1.getLevel() - o2.getLevel();
    }

}




class TerritoryNameDrawable implements IDrawable
{
    private final String m_territoryName;

    public TerritoryNameDrawable(final String territoryName)
    {
        this.m_territoryName = territoryName;
    }

    
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        Territory territory = data.getMap().getTerritory(m_territoryName);

        if (territory.isWater())
            return;

        Rectangle territoryBounds = mapData.getBoundingRect(territory);
        graphics.setFont(MapImage.MAP_FONT);

        TerritoryAttachment ta = TerritoryAttachment.get(territory);
        graphics.setColor(Color.black);
        FontMetrics fm = graphics.getFontMetrics();
        int x = territoryBounds.x;
        int y = territoryBounds.y;

        x += (int) territoryBounds.getWidth() >> 1;
        y += (int) territoryBounds.getHeight() >> 1;

        x -= fm.stringWidth(territory.getName()) >> 1;
        y += fm.getAscent() >> 1;

        if(mapData.drawTerritoryNames())
            graphics.drawString(territory.getName(), x - bounds.x, y - bounds.y);

        //draw the ipcs.
        if (ta.getProduction() > 0)
        {
            String prod = new Integer(ta.getProduction()).toString();
            Point place = mapData.getIPCPlacementPoint(territory);
            //if ipc_place.txt is specified draw there
            if(place != null)
            {
                graphics.drawString(prod, place.x - bounds.x, place.y - bounds.y);                
            }
            //otherwise, draw under the territory name
            else
            {
                x = territoryBounds.x + ((((int) territoryBounds.getWidth()) - fm.stringWidth(prod)) >> 1);
                y += fm.getLeading() + fm.getAscent();
                graphics.drawString(prod, x - bounds.x, y - bounds.y);
                
            }
                
            
 
            

        }

    }

    public int getLevel()
    {
        return TERRITORY_TEXT_LEVEL;
    }
}

class VCDrawable implements IDrawable
{

    private final Territory m_location;
    
    
    public VCDrawable(final Territory location)
    {
        m_location = location;
    }
    


    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        Point point = mapData.getVCPlacementPoint(m_location);
        graphics.drawImage(mapData.getVCImage(), point.x - bounds.x, point.y - bounds.y, null);
        
    }

    public int getLevel()
    {
       return VC_MARKER_LEVEL;
    }
    
}


class CapitolMarkerDrawable implements IDrawable
{

    private final String m_player;
    private final String m_location;
    private final UIContext m_uiContext;
    
    
    public CapitolMarkerDrawable(final PlayerID player, final Territory location, UIContext uiContext)
    {
        super();
        m_player = player.getName();
        m_location = location.getName();
        m_uiContext = uiContext;
    }


    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        Image img = m_uiContext.getFlagImageFactory().getLargeFlag(data.getPlayerList().getPlayerID(m_player));
        Point point = mapData.getCapitolMarkerLocation(data.getMap().getTerritory(m_location));
        
        graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
        
    }

    public int getLevel()
    {
       return CAPITOL_MARKER_LEVEL;
    }
    
}

abstract class MapTileDrawable implements IDrawable
{
    
    protected boolean m_noImage = false;
    protected final int m_x;
    protected final int m_y;
  
    public MapTileDrawable(final int x, final int y)
    {
        m_x = x;
        m_y = y;
    }

    protected abstract Image getImage();
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {

        Image img = getImage();
        
        if(img == null)
            return;
    
        Object oldValue = graphics.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        
        Stopwatch drawStopWatch = new Stopwatch(s_logger, Level.FINEST, "drawing images");
        graphics.drawImage(img, m_x * TileManager.TILE_SIZE - bounds.x, m_y * TileManager.TILE_SIZE - bounds.y,null);
        drawStopWatch.done();
        
        if(oldValue == null)
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
        else
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldValue);

    }

    
}

class ReliefMapDrawable extends MapTileDrawable
{
    private final UIContext m_context;

    public ReliefMapDrawable(int x, int y, UIContext context)
    {
        super(x, y);
        m_context = context;
    }
    
    
    protected Image getImage()
    {
        if(m_noImage)
            return null;
        
        if(!TileImageFactory.getShowReliefImages())
            return null;
        
        Image rVal = m_context.getTileImageFactory().getReliefTile(m_x, m_y);
        if(rVal == null)
            m_noImage = true;
        
        return rVal;
    }

 
    

    public int getLevel()
    {
        return RELIEF_LEVEL;
    }
    
}

class BaseMapDrawable extends MapTileDrawable
{
    
    private final UIContext m_uiContext;
   
    public BaseMapDrawable(final int x, final int y, UIContext context)
    {
        super(x,y);
        m_uiContext = context;
    }


   
    protected Image getImage()
    {
        if(m_noImage)
            return null;

        Image rVal = m_uiContext.getTileImageFactory().getBaseTile(m_x, m_y); 
        if(rVal == null)
            m_noImage = true;
        
        return rVal;
    }


    public int getLevel()
    {
        return BASE_MAP_LEVEL;
    }
}

// for water or land
class LandTerritoryDrawable implements IDrawable
{
    private final String m_territoryName;
    private final boolean m_isWater;

    public LandTerritoryDrawable(final String territoryName)
    {
        m_territoryName = territoryName;
        m_isWater = false;
    }

    public LandTerritoryDrawable(final String territoryName, final boolean isWater)
    {
        m_territoryName = territoryName;
        m_isWater = isWater;
    }

    
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {

        Territory territory = data.getMap().getTerritory(m_territoryName);
        Color territoryColor;

        if (TerritoryAttachment.get(territory).isImpassible())
        {
          territoryColor = mapData.impassibleColor();
        }
        else
        {
            territoryColor = mapData.getPlayerColor(territory.getOwner().getName()); 
            if(m_isWater)
                territoryColor = new Color(territoryColor.getRed(), territoryColor.getGreen(), territoryColor.getBlue(), 220);
        }

        List polys = mapData.getPolygons(territory);
        
        Iterator iter2 = polys.iterator();
        while (iter2.hasNext())
        {
            Polygon polygon = (Polygon) iter2.next();
            
            //if we dont have to draw, dont
            if(!polygon.intersects(bounds) && !polygon.contains(bounds))
                continue;
            
            //use a copy since we will move the polygon
            polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);

            polygon.translate(-bounds.x, -bounds.y);
            graphics.setColor(territoryColor);
            graphics.fillPolygon(polygon);
            graphics.setColor(Color.BLACK);
            graphics.drawPolygon(polygon);
        }
        
    }

    public int getLevel()
    {
        if(m_isWater)
            return BASE_MAP_LEVEL;
        return POLYGONS_LEVEL;
    }

}

