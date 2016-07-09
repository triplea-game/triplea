package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.mapdata.MapData;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

public class BlockadeZoneDrawable implements IDrawable {
  private final String m_location;

  // private final UIContext m_uiContext;
  public BlockadeZoneDrawable(final Territory location, final IUIContext uiContext) {
    super();
    m_location = location.getName();
    // m_uiContext = uiContext;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    // Find blockade.png from misc folder
    final Point point = mapData.getBlockadePlacementPoint(data.getMap().getTerritory(m_location));
    drawImage(graphics, mapData.getBlockadeImage(), point, bounds);
  }

  @Override
  public int getLevel() {
    return CAPITOL_MARKER_LEVEL;
  }
}
