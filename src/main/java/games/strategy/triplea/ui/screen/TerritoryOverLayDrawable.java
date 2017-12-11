package games.strategy.triplea.ui.screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable;

class TerritoryOverLayDrawable implements IDrawable {
  enum Operation {
    FILL, DRAW
  }

  private final String territoryName;
  private final Color color;
  private final Operation operation;

  TerritoryOverLayDrawable(final Color color, final String name, final Operation operation) {
    this.color = color;
    territoryName = name;
    this.operation = operation;
  }

  TerritoryOverLayDrawable(final Color color, final String name, final int alpha, final Operation operation) {
    this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    territoryName = name;
    this.operation = operation;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    final Territory territory = data.getMap().getTerritory(territoryName);
    final List<Polygon> polys = mapData.getPolygons(territory);
    graphics.setColor(color);
    for (Polygon polygon : polys) {
      // if we dont have to draw, dont
      if (!polygon.intersects(bounds) && !polygon.contains(bounds)) {
        continue;
      }
      // use a copy since we will move the polygon
      polygon = new Polygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
      polygon.translate(-bounds.x, -bounds.y);
      if (operation == Operation.FILL) {
        graphics.fillPolygon(polygon);
      } else {
        graphics.drawPolygon(polygon);
      }
    }
  }

  @Override
  public int getLevel() {
    return TERRITORY_OVERLAY_LEVEL;
  }
}
