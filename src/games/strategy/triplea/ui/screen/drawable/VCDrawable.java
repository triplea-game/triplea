package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

class VCDrawable implements IDrawable {
  private final Territory location;

  public VCDrawable(final Territory location) {
    this.location = location;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {

    final Point point = mapData.getVCPlacementPoint(location);
    drawImage(graphics, mapData.getVCImage(), point, bounds);
  }

  @Override
  public int getLevel() {
    return VC_MARKER_LEVEL;
  }
}
