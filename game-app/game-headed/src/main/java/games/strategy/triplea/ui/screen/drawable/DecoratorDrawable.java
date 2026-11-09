package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

/** Draws a custom image at a given point on the map. */
public class DecoratorDrawable extends AbstractDrawable {
  private final Point point;
  private final Image image;

  public DecoratorDrawable(final Point point, final Image image) {
    this.point = point;
    this.image = image;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    graphics.drawImage(image, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.DECORATOR_LEVEL;
  }
}
