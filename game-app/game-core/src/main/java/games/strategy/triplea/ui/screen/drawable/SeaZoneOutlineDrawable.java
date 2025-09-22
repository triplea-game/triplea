package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.ui.Util;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;

/**
 * Draws a black outline around the associated territory. Intended only for use with water
 * territories (sea zones).
 */
public class SeaZoneOutlineDrawable extends AbstractDrawable {
  private final String territoryName;

  public SeaZoneOutlineDrawable(final String territoryName) {
    this.territoryName = territoryName;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    final Territory territory = data.getMap().getTerritoryOrNull(territoryName);
    final List<Polygon> polys = mapData.getPolygons(territory);
    graphics.setColor(Color.BLACK);
    for (final Polygon polygon : polys) {
      if (!polygon.intersects(bounds) && !polygon.contains(bounds)) {
        continue;
      }

      graphics.drawPolygon(Util.translatePolygon(polygon, -bounds.x, -bounds.y));
    }
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.POLYGONS_LEVEL;
  }
}
