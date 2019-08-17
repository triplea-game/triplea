package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.ui.Util;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;

/**
 * Superclass for {@link IDrawable} implementations that draws a black outline around a territory
 * and uses an instance of {@link Paint} provided by the subclass to fill the territory interior.
 */
public abstract class TerritoryDrawable extends AbstractDrawable {
  protected TerritoryDrawable() {}

  protected static void draw(
      final Rectangle bounds,
      final Graphics2D graphics,
      final MapData mapData,
      final Territory territory,
      final Paint territoryPaint) {
    final List<Polygon> polygons = mapData.getPolygons(territory);
    for (final Polygon polygon : polygons) {
      if (!polygon.intersects(bounds) && !polygon.contains(bounds)) {
        continue;
      }

      final Polygon translatedPolygon = Util.translatePolygon(polygon, -bounds.x, -bounds.y);
      graphics.setPaint(territoryPaint);
      graphics.fillPolygon(translatedPolygon);
      graphics.setColor(Color.BLACK);
      graphics.drawPolygon(translatedPolygon);
    }
  }
}
