package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.List;

public class SeaZoneOutlineDrawable implements IDrawable {
  private final String m_territoryName;

  public SeaZoneOutlineDrawable(final String territoryName) {
    m_territoryName = territoryName;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    final Territory territory = data.getMap().getTerritory(m_territoryName);
    final List<Polygon> polys = mapData.getPolygons(territory);
    final Iterator<Polygon> iter2 = polys.iterator();
    while (iter2.hasNext()) {
      Polygon polygon = iter2.next();
      // if we dont have to draw, dont
      if (!polygon.intersects(bounds) && !polygon.contains(bounds)) {
        continue;
      }
      // use a copy since we will move the polygon
      polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
      polygon.translate(-bounds.x, -bounds.y);
      graphics.setColor(Color.BLACK);
      graphics.drawPolygon(polygon);
    }
  }

  @Override
  public int getLevel() {
    return POLYGONS_LEVEL;
  }
}
