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

import java.awt.*;
import java.util.*;
import java.util.Comparator;
import java.util.List;

/**
 * @author Sean Bridges
 */
public interface Drawable
{
    public static final int BASE_MAP_LEVEL = 1;
    public static final int POLYGONS_LEVEL = 2;
    public static final int RELIEF_LEVEL = 3;
    public static final int TERRITORY_TEXT_LEVEL = 4;
    public static final int UNITS_LEVEL = 5;

    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData);

    public int getLevel();
}

class DrawableComparator implements Comparator
{

    public int compare(Object o1, Object o2)
    {
        return ((Drawable) o1).getLevel() - ((Drawable) o2).getLevel();
    }

}

class TerritoryNameDrawable implements Drawable
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

class ReliefMapDrawable implements Drawable
{

    private final int m_x;
    private final int m_y;
    
    public ReliefMapDrawable(final int x, final int y)
    {
        m_x = x;
        m_y = y;
    }
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        if(!TileImageFactory.getShowReliefImages())
            return;
        
        Image img = TileImageFactory.getInstance().getReliefTile(m_x, m_y);
        if(img != null)
        {
            Object oldValue = graphics.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
            graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

            graphics.drawImage(img, m_x * TileManager.TILE_SIZE - bounds.x, m_y * TileManager.TILE_SIZE - bounds.y,null);
            
            if(oldValue == null)
                graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
            else
                graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldValue);
        }
    }

    public int getLevel()
    {
        return RELIEF_LEVEL;
    }
    
}

class BaseMapDrawable implements Drawable
{

    private final int m_x;
    private final int m_y;
    
    public BaseMapDrawable(final int x, final int y)
    {
        m_x = x;
        m_y = y;
    }
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData)
    {
        Image img = TileImageFactory.getInstance().getBaseTile(m_x, m_y);
        if(img != null)
            graphics.drawImage(img, m_x * TileManager.TILE_SIZE - bounds.x, m_y * TileManager.TILE_SIZE - bounds.y,null);
        
    }

    public int getLevel()
    {
        return BASE_MAP_LEVEL;
    }
    
}

class LandTerritoryDrawable implements Drawable
{
    private final String m_territoryName;

    public LandTerritoryDrawable(final String territoryName)
    {
        m_territoryName = territoryName;
    }

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

class UnitsDrawer implements Drawable
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

        Image img = UnitIconImageFactory.instance().getImage(type, owner, data, m_damaged);
        graphics.drawImage(img, m_placementPoint.x - bounds.x, m_placementPoint.y - bounds.y, null);

        if (m_count != 1)
        {
            graphics.setColor(Color.white);
            graphics.setFont(MapImage.MAP_FONT);
            graphics.drawString(String.valueOf(m_count), m_placementPoint.x - bounds.x + (UnitIconImageFactory.instance().getUnitImageWidth() / 4),
                    m_placementPoint.y - bounds.y + UnitIconImageFactory.instance().getUnitImageHeight());
        }
    }
    

    public int getLevel()
    {
        return UNITS_LEVEL;
    }

}