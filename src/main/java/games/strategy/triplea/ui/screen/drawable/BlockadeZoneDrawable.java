package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;

public class BlockadeZoneDrawable implements IDrawable {
  private final String location;

  public BlockadeZoneDrawable(final Territory location) {
    super();
    this.location = location.getName();
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    // Find blockade.png from misc folder
    final Point point = mapData.getBlockadePlacementPoint(data.getMap().getTerritory(location));
    drawImage(graphics, mapData.getBlockadeImage(), point, bounds);
  }

  @Override
  public int getLevel() {
    return CAPITOL_MARKER_LEVEL;
  }
}
