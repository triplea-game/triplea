package games.strategy.triplea.ui.screen.drawable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.ui.Util;

public abstract class TerritoryDrawable {
  protected static void draw(final Rectangle bounds, final Graphics2D graphics, final MapData mapData,
      final Territory territory, final Paint territoryPaint) {
    final List<Polygon> polys = mapData.getPolygons(territory);
    for (final Polygon polygon : polys) {
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
