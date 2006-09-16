package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.*;
import games.strategy.triplea.ui.MapData;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

public class TerritoryOverLayDrawable implements IDrawable
{
    
    private final String m_territoryName;
    private final Color m_color;
    
    
    public TerritoryOverLayDrawable(Color color, String name, int alpha)
    {
        m_color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        m_territoryName = name;
    }
 

    public void prepare()
    {
        
    }

    
    
    public void draw(Rectangle bounds, GameData data, Graphics2D graphics, MapData mapData, AffineTransform unscaled, AffineTransform scaled)
    {
        Territory territory = data.getMap().getTerritory(m_territoryName);
       
        List<Polygon> polys = mapData.getPolygons(territory);
        
        graphics.setColor(m_color);
        
        Iterator<Polygon>  polyIter = polys.iterator();
        while (polyIter.hasNext())
        {
            Polygon polygon = polyIter.next();
            
            //if we dont have to draw, dont
            if(!polygon.intersects(bounds) && !polygon.contains(bounds))
                continue;
            
            //use a copy since we will move the polygon
            polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);

            polygon.translate(-bounds.x, -bounds.y);
            
            graphics.fillPolygon(polygon);
        }
        
    }

    public int getLevel()
    {
        return TERRITORY_OVERLAY_LEVEL;
    }
    
}
