package games.strategy.triplea.ui.screen.drawable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.ui.Util;

/**
 * Draws a black outline around the associated territory.
 */
public class OptionalExtraTerritoryBordersDrawable implements IDrawable {
  private final String territoryName;
  private final OptionalExtraBorderLevel level;

  public OptionalExtraTerritoryBordersDrawable(final String territoryName, final OptionalExtraBorderLevel level) {
    this.territoryName = territoryName;
    this.level = level;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled) {
    final Territory territory = data.getMap().getTerritory(territoryName);
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
  public int getLevel() {
    if (level == OptionalExtraBorderLevel.HIGH) {
      return OPTIONAL_EXTRA_TERRITORY_BORDERS_HIGH_LEVEL;
    }
    return OPTIONAL_EXTRA_TERRITORY_BORDERS_MEDIUM_LEVEL;
  }
}
