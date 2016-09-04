package games.strategy.triplea.ui.screen.drawable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;

public abstract class TerritoryDrawable {
  protected static void draw(final Rectangle bounds, final Graphics2D graphics, final MapData mapData,
      final AffineTransform scaled, final Territory territory,
      final Paint territoryPaint) {
    final List<Polygon> polys = mapData.getPolygons(territory);
    for (Polygon polygon : polys) {
      // if we dont have to draw, dont
      if (!polygon.intersects(bounds) && !polygon.contains(bounds)) {
        continue;
      }
      // use a copy since we will move the polygon
      polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
      polygon.translate(-bounds.x, -bounds.y);
      graphics.setPaint(territoryPaint);
      graphics.fillPolygon(polygon);
      graphics.setColor(Color.BLACK);
      graphics.drawPolygon(polygon);
    }
  }
}
