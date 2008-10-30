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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.MapData;
import games.strategy.triplea.ui.UIContext;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author Sean Bridges
 */
public interface IDrawable
{
    public Logger s_logger = Logger.getLogger(IDrawable.class.getName());

    public static final int BASE_MAP_LEVEL = 1;

    public static final int POLYGONS_LEVEL = 2;

    public static final int RELIEF_LEVEL = 3;
    
    public static final int CONVOY_LEVEL = 4;

    public static final int CAPITOL_MARKER_LEVEL = 7;

    public static final int VC_MARKER_LEVEL = 8;
    
    public static final int DECORATOR_LEVEL = 9;
    

    public static final int TERRITORY_TEXT_LEVEL = 10;

    public static final int UNITS_LEVEL = 11;

    public static final int TERRITORY_OVERLAY_LEVEL = 12;

    /**
     * Draw the tile
     * 
     * If the graphics are scaled, then unscaled and scaled will be non null.<p>
     * 
     * The affine transform will be set to the scaled version.
     * 
     * 
     */
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled);

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
    private final UIContext m_uiContext;

    public TerritoryNameDrawable(final String territoryName, UIContext context)
    {
        this.m_territoryName = territoryName;
        this.m_uiContext = context;
    }

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {
        Territory territory = data.getMap().getTerritory(m_territoryName);
        TerritoryAttachment ta = TerritoryAttachment.get(territory);
       
        boolean drawComments = false;
        String commentText = null;

        if (territory.isWater())
        {
        	if (ta == null)
        	return;
        	
        	if (ta.isConvoyRoute())
        	{
        		drawComments = true;
        		commentText = ta.getConvoyAttached() + " Convoy Route";
        	}

        	//Check to ensure there's an original owner to fix abend
        	if (ta.getProduction() > 0 && ta.getOriginalOwner() != null)   		
        	{
        		drawComments = true;
        		commentText = ta.getOriginalOwner().getName() + " Convoy Center";
        	}
        	
        	if (drawComments == false)
        	{
        		return;
        	}
        }

        Rectangle territoryBounds = mapData.getBoundingRect(territory);
        graphics.setFont(MapImage.MAP_FONT);

        graphics.setColor(Color.black);
        FontMetrics fm = graphics.getFontMetrics();
        int x;
        int y;
        
        //if we specify a placement point, use it
        //otherwise, put it in the center
        Point namePlace = mapData.getNamePlacementPoint(territory);
        if(namePlace == null)
        {
            x = territoryBounds.x;
            y = territoryBounds.y;
    
            x += (int) territoryBounds.getWidth() >> 1;
            y += (int) territoryBounds.getHeight() >> 1;
    
            x -= fm.stringWidth(territory.getName()) >> 1;
            y += fm.getAscent() >> 1;
        }
        else
        {
            x = namePlace.x;
            y = namePlace.y;
        }

        if(mapData.drawTerritoryNames() && mapData.shouldDrawTerritoryName(m_territoryName))
        	if (drawComments)
        	{
        		graphics.drawString(commentText, x - bounds.x, y - bounds.y);
        	}
        	else
        	{
                graphics.drawString(territory.getName(), x - bounds.x, y - bounds.y);
        	}

        // draw the ipcs.
        if (ta.getProduction() > 0)
        {
            Image img = m_uiContext.getIPCImageFactory().getIpcImage(ta.getProduction());
            String prod = Integer.valueOf(ta.getProduction()).toString();
            
            Point place = mapData.getIPCPlacementPoint(territory);
            // if ipc_place.txt is specified draw there
            if(place != null)
            {
                x = place.x;
                y = place.y;
                
                draw(bounds, graphics, x, y, img, prod);
                                
            }
            // otherwise, draw under the territory name
            else
            {
                x = x + ((fm.stringWidth(m_territoryName)) >> 1)  -    ((fm.stringWidth(prod)) >> 1);
                y += fm.getLeading() + fm.getAscent();
                
                draw(bounds, graphics, x, y, img, prod);
                
                
            }
                
            
 
            

        }

    }

    private void draw(Rectangle bounds, Graphics2D graphics, int x, int y, Image img, String prod)
    {
        if(img == null)
        {
            graphics.drawString(prod, x - bounds.x, y - bounds.y);
        }
        else
        {
            //we want to be consistent
            //drawString takes y as the base line position
            //drawImage takes x as the top right corner
            y -= img.getHeight(null);
            graphics.drawImage(img, x - bounds.x, y - bounds.y,null);
        }
    }    public int getLevel()
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

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {
        Point point = mapData.getVCPlacementPoint(m_location);
        graphics.drawImage(mapData.getVCImage(), point.x - bounds.x, point.y - bounds.y, null);

    }

    public int getLevel()
    {
        return VC_MARKER_LEVEL;
    }

}

class DecoratorDrawable implements IDrawable
{

    private final Point m_point;
    private final Image m_image;
    
    
    
    public DecoratorDrawable(final Point point, final Image image)
    {
        super();
        m_point = point;
        m_image = image;
    }

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {
        graphics.drawImage(m_image, m_point.x - bounds.x, m_point.y - bounds.y, null);
    }

    public int getLevel()
    {
        return DECORATOR_LEVEL;
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

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {
        // Changed back to use Large flags
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
    protected final UIContext m_uiContext;
    protected boolean m_unscaled;
    
    public MapTileDrawable(final int x, final int y, UIContext context)
    {
        m_x = x;
        m_y = y;
        m_uiContext = context;
        m_unscaled = false;
    }

    public abstract MapTileDrawable getUnscaledCopy();
    
    protected abstract Image getImage();

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {
        Image img = getImage();

        if (img == null)
            return;

        Object oldValue = graphics.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        //the tile images are already scaled
        if(unscaled != null)
            graphics.setTransform(unscaled);
        
        Stopwatch drawStopWatch = new Stopwatch(s_logger, Level.FINEST, "drawing tile images");
       
        graphics.drawImage(img, m_x * TileManager.TILE_SIZE - bounds.x, m_y * TileManager.TILE_SIZE - bounds.y, null);
        drawStopWatch.done();

        if(unscaled != null)
            graphics.setTransform(scaled);
        if (oldValue == null)
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
        else
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldValue);
    }        

}

class ReliefMapDrawable extends MapTileDrawable
{
    

    public ReliefMapDrawable(int x, int y, UIContext context)
    {
        super(x, y, context);
        
    }

    public MapTileDrawable getUnscaledCopy()
    {
       ReliefMapDrawable copy = new ReliefMapDrawable(m_x, m_y, m_uiContext);
       copy.m_unscaled = true;
       return copy;
    }
    
    protected Image getImage()
    {
        if (m_noImage)
            return null;

        if (!TileImageFactory.getShowReliefImages())
            return null;

        Image rVal;
        if(m_unscaled)
            rVal = m_uiContext.getTileImageFactory().getUnscaledUncachedReliefTile(m_x, m_y);
        else
            rVal = m_uiContext.getTileImageFactory().getReliefTile(m_x, m_y);
        
        if (rVal == null)
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

    public BaseMapDrawable(final int x, final int y, UIContext context)
    {
        super(x, y, context);
    }

    public MapTileDrawable getUnscaledCopy()
    {
        BaseMapDrawable copy = new BaseMapDrawable(m_x, m_y, m_uiContext);
       copy.m_unscaled = true;
       return copy;
    }

    
    protected Image getImage()
    {
        if (m_noImage)
            return null;

        Image rVal;
        
        if(m_unscaled)
            rVal = m_uiContext.getTileImageFactory().getUnscaledUncachedBaseTile(m_x, m_y);
        else
            rVal = m_uiContext.getTileImageFactory().getBaseTile(m_x, m_y);
        
        if (rVal == null)
            m_noImage = true;

        return rVal;
    }

    public int getLevel()
    {
        return BASE_MAP_LEVEL;
    }
    
    
}

// Rewritten class to use country markers rather than shading for Convoy Centers/Routes.
class ConvoyZoneDrawable implements IDrawable
{
    private final String m_player;
    private final String m_location;
    private final UIContext m_uiContext;
    
    public ConvoyZoneDrawable(final PlayerID player, final Territory location, UIContext uiContext)
    {
        super();
        m_player = player.getName();
        m_location = location.getName();
        m_uiContext = uiContext;
    }

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {
        Image img = m_uiContext.getFlagImageFactory().getFlag(data.getPlayerList().getPlayerID(m_player));
        Point point = mapData.getCapitolMarkerLocation(data.getMap().getTerritory(m_location));
        graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
    }

    public int getLevel()
    {
        return CAPITOL_MARKER_LEVEL;
    }

}

//Class to use 'Faded' country markers for Kamikaze Zones.
class KamikazeZoneDrawable implements IDrawable
{
 private final String m_player;
 private final String m_location;
 private final UIContext m_uiContext;
 
 public KamikazeZoneDrawable(final PlayerID player, final Territory location, UIContext uiContext)
 {
     super();
     m_player = player.getName();
     m_location = location.getName();
     m_uiContext = uiContext;
 }

 public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
 {
	 //Change so only original owner gets the kamikazi zone marker
	 Territory terr = data.getMap().getTerritory(m_location);     
	 Image img = m_uiContext.getFlagImageFactory().getFadedFlag(data.getPlayerList().getPlayerID(TerritoryAttachment.get(terr).getOriginalOwner().getName()));
     Point point = mapData.getKamikazeMarkerLocation(data.getMap().getTerritory(m_location));
     graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
 }

 public int getLevel()
 {
     return CAPITOL_MARKER_LEVEL;
 }
}

class SeaZoneOutlineDrawable implements IDrawable
{
    private final String m_territoryName;

    public SeaZoneOutlineDrawable(final String territoryName)
    {
        m_territoryName = territoryName;
    }

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {

        Territory territory = data.getMap().getTerritory(m_territoryName);
                List polys = mapData.getPolygons(territory);

        Iterator iter2 = polys.iterator();
        while (iter2.hasNext())
        {
            Polygon polygon = (Polygon) iter2.next();

            // if we dont have to draw, dont
            if (!polygon.intersects(bounds) && !polygon.contains(bounds))
                continue;

            // use a copy since we will move the polygon
            polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);

            polygon.translate(-bounds.x, -bounds.y);
            graphics.setColor(Color.BLACK);
            graphics.drawPolygon(polygon);
        }

    }

    public int getLevel()
    {
        return POLYGONS_LEVEL;
    }
    
}

class LandTerritoryDrawable implements IDrawable
{
    private final String m_territoryName;

    public LandTerritoryDrawable(final String territoryName)
    {
        m_territoryName = territoryName;
    }

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {

        Territory territory = data.getMap().getTerritory(m_territoryName);
        Color territoryColor;

        if (TerritoryAttachment.get(territory).isImpassible())
        {
            territoryColor = mapData.impassibleColor();
        } else
        {
            territoryColor = mapData.getPlayerColor(territory.getOwner().getName());
        }

        List<Polygon> polys = mapData.getPolygons(territory);
        
        Object oldAAValue = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        //at 100% scale, this makes the lines look worse
        if(!(scaled == unscaled))
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for(Polygon polygon : polys)
        {
            // if we dont have to draw, dont
            if (!polygon.intersects(bounds) && !polygon.contains(bounds))
                continue;

            // use a copy since we will move the polygon
            polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);

            polygon.translate(-bounds.x, -bounds.y);
            graphics.setColor(territoryColor);
            graphics.fillPolygon(polygon);
            graphics.setColor(Color.BLACK);
            graphics.drawPolygon(polygon);
        }
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAAValue);
    }

    public int getLevel()
    {
        return POLYGONS_LEVEL;
    }

}
