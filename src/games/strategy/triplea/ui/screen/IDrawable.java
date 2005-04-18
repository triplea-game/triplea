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
import games.strategy.engine.data.GameData;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.image.*;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.ui.MapData;
import games.strategy.triplea.util.Stopwatch;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.Comparator;
import java.util.List;
import java.util.logging.*;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

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

    /**
     * Start any asynchronous preparation that can be done in the backgrounds.
     * Note, a drawable should not rely on prepare being called.  It is merly a hint.
     */
    public void prepare();
    
    /**
     * Draw the tile
     */
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData);

    public int getLevel();
}

class DrawableComparator implements Comparator
{

    public int compare(Object o1, Object o2)
    {
        return ((IDrawable) o1).getLevel() - ((IDrawable) o2).getLevel();
    }

}

class TerritoryNameDrawable implements IDrawable
{
    private final String m_territoryName;

    public TerritoryNameDrawable(final String territoryName)
    {
        this.m_territoryName = territoryName;
    }

    public void prepare() {}
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        Territory territory = data.getMap().getTerritory(m_territoryName);

        if (territory.isWater())
            return;

        Rectangle territoryBounds = mapData.getBoundingRect(territory);
        graphics.setFont(MapImage.MAP_FONT);

        TerritoryAttatchment ta = TerritoryAttatchment.get(territory);
        graphics.setColor(Color.black);
        FontMetrics fm = graphics.getFontMetrics();
        int x = territoryBounds.x;
        int y = territoryBounds.y;

        x += (int) territoryBounds.getWidth() >> 1;
        y += (int) territoryBounds.getHeight() >> 1;

        x -= fm.stringWidth(territory.getName()) >> 1;
        y += fm.getAscent() >> 1;

        graphics.drawString(territory.getName(), x - bounds.x, y - bounds.y);

        if (ta.getProduction() > 0)
        {
            String prod = new Integer(ta.getProduction()).toString();
            x = territoryBounds.x + ((((int) territoryBounds.getWidth()) - fm.stringWidth(prod)) >> 1);
            y += fm.getLeading() + fm.getAscent();
            graphics.drawString(prod, x - bounds.x, y - bounds.y);
        }

    }

    public int getLevel()
    {
        return TERRITORY_TEXT_LEVEL;
    }
}

class VCDrawable implements IDrawable
{

    static Image s_vcImage;
    
    static
    {
        URL url = ClassLoader.getSystemClassLoader().getResource("games/strategy/triplea/image/images/vc.png");
        if(url == null)
            throw new IllegalStateException("Could not load vc image");
        
        try
        {
            s_vcImage = ImageIO.read(url);
        } catch (IOException e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
    }
    
    private final Territory m_location;
    
    
    public VCDrawable(final Territory location)
    {
        m_location = location;
    }
    
    public void prepare()
    {
    }

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        Point point = mapData.getVCPlacementPoint(m_location);
        graphics.drawImage(s_vcImage, point.x - bounds.x, point.y - bounds.y, null);
        
    }

    public int getLevel()
    {
       return VC_MARKER_LEVEL;
    }
    
}


class CapitolMarkerDrawable implements IDrawable
{

    private final PlayerID m_player;
    private final Territory m_location;
    
    
    public CapitolMarkerDrawable(final PlayerID player, final Territory location)
    {
        super();
        m_player = player;
        m_location = location;
    }
    public void prepare()
    {
    }

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        Image img = FlagIconImageFactory.instance().getLargeFlag(m_player);
        Point point = mapData.getCapitolMarkerLocation(m_location);
        
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

    public ReliefMapDrawable(int x, int y)
    {
        super(x, y);
    }
    
    public void prepare() 
    {
        if(!TileImageFactory.getShowReliefImages())
            return;
        if(m_noImage)
            return;
        
        TileImageFactory.getInstance().prepareReliefTile(m_x, m_y);
    }
    
    protected Image getImage()
    {
        if(m_noImage)
            return null;
        
        if(!TileImageFactory.getShowReliefImages())
            return null;
        
        Image rVal = TileImageFactory.getInstance().getReliefTile(m_x, m_y);
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
   
    public BaseMapDrawable(final int x, final int y)
    {
        super(x,y);
    }
   
    public void prepare() 
    {
        if(m_noImage)
            return;
        
        TileImageFactory.getInstance().prepareBaseTile(m_x, m_y);
    }

   
    protected Image getImage()
    {
        if(m_noImage)
            return null;

        Image rVal = TileImageFactory.getInstance().getBaseTile(m_x, m_y); 
        if(rVal == null)
            m_noImage = true;
        
        return rVal;
    }


    public int getLevel()
    {
        return BASE_MAP_LEVEL;
    }
}

class LandTerritoryDrawable implements IDrawable
{
    private final String m_territoryName;

    public LandTerritoryDrawable(final String territoryName)
    {
        m_territoryName = territoryName;
    }

    public void prepare() {}
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {

        Territory territory = data.getMap().getTerritory(m_territoryName);
        Color territoryColor;

        if (TerritoryAttatchment.get(territory).isImpassible())
        {
          territoryColor = mapData.impassibleColor();
        }
        else
        {
          territoryColor = mapData.getPlayerColor(territory.getOwner().getName()); 
        }

        List polys = MapData.getInstance().getPolygons(territory);
        
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
        return POLYGONS_LEVEL;
    }

}

class UnitsDrawer implements IDrawable
{
    private final int m_count;
    private final String m_unitType;
    private final String m_playerName;
    private final Point m_placementPoint;
    private final boolean m_damaged;

    public UnitsDrawer(final int count, final String unitType, final String playerName, final Point placementPoint, final boolean damaged)
    {
        m_count = count;
        m_unitType = unitType;
        m_playerName = playerName;
        m_placementPoint = placementPoint;
        m_damaged = damaged;
    }

    public void prepare() {}
    
    public Point getPlacementPoint()
    {
        return m_placementPoint;
    }
    
    public String getPlayer()
    {
        return m_playerName;
    }
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        UnitType type = data.getUnitTypeList().getUnitType(m_unitType);
        if (type == null)
            throw new IllegalStateException("Type not found:" + m_unitType);
        PlayerID owner = data.getPlayerList().getPlayerID(m_playerName);

        Image img = UnitImageFactory.instance().getImage(type, owner, data, m_damaged);
        graphics.drawImage(img, m_placementPoint.x - bounds.x, m_placementPoint.y - bounds.y, null);

        if (m_count != 1)
        {
            graphics.setColor(Color.white);
            graphics.setFont(MapImage.MAP_FONT);
            graphics.drawString(String.valueOf(m_count), m_placementPoint.x - bounds.x + (UnitImageFactory.instance().getUnitImageWidth() / 4),
                    m_placementPoint.y - bounds.y + UnitImageFactory.instance().getUnitImageHeight());
        }
    }
    

    public int getLevel()
    {
        return UNITS_LEVEL;
    }

}