package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.MapData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.List;

public class TerritoryOverLayDrawable implements IDrawable
{
	public static enum OP
	{
		FILL, DRAW
	};
	
	private final String m_territoryName;
	private final Color m_color;
	private final OP m_op;
	
	public TerritoryOverLayDrawable(final Color color, final String name, final OP op)
	{
		m_color = color;
		m_territoryName = name;
		m_op = op;
	}
	
	public TerritoryOverLayDrawable(final Color color, final String name, final int alpha, final OP op)
	{
		m_color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
		m_territoryName = name;
		m_op = op;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		final Territory territory = data.getMap().getTerritory(m_territoryName);
		final List<Polygon> polys = mapData.getPolygons(territory);
		graphics.setColor(m_color);
		final Iterator<Polygon> polyIter = polys.iterator();
		while (polyIter.hasNext())
		{
			Polygon polygon = polyIter.next();
			// if we dont have to draw, dont
			if (!polygon.intersects(bounds) && !polygon.contains(bounds))
				continue;
			// use a copy since we will move the polygon
			polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
			polygon.translate(-bounds.x, -bounds.y);
			if (m_op == OP.FILL)
				graphics.fillPolygon(polygon);
			else
				graphics.drawPolygon(polygon);
		}
	}
	
	public int getLevel()
	{
		return TERRITORY_OVERLAY_LEVEL;
	}
}
