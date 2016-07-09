package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.mapdata.MapData;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

class DecoratorDrawable implements IDrawable {
  private final Point m_point;
  private final Image m_image;

  public DecoratorDrawable(final Point point, final Image image) {
    super();
    m_point = point;
    m_image = image;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    graphics.drawImage(m_image, m_point.x - bounds.x, m_point.y - bounds.y, null);
  }

  @Override
  public int getLevel() {
    return DECORATOR_LEVEL;
  }
}
