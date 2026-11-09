package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/** Draws the blockade image for the associated territory. */
public class BlockadeZoneDrawable extends AbstractDrawable {
  private final String location;

  public BlockadeZoneDrawable(final Territory location) {
    this.location = location.getName();
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    // Find blockade.png from misc folder
    final Point point =
        mapData.getBlockadePlacementPoint(data.getMap().getTerritoryOrNull(location));
    drawImage(graphics, mapData.getBlockadeImage(), point, bounds);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.CAPITOL_MARKER_LEVEL;
  }
}
