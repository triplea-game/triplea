package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.mapdata.MapData;

public class DecoratorDrawable implements IDrawable {
  private final Point point;
  private final Image image;

  public DecoratorDrawable(final Point point, final Image image) {
    super();
    this.point = point;
    this.image = image;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    graphics.drawImage(image, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public int getLevel() {
    return DECORATOR_LEVEL;
  }
}
