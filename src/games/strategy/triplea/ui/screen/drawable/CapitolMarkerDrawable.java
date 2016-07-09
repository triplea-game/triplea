package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.mapdata.MapData;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

class CapitolMarkerDrawable implements IDrawable {
  private final String m_player;
  private final String m_location;
  private final IUIContext m_uiContext;

  public CapitolMarkerDrawable(final PlayerID player, final Territory location, final IUIContext uiContext) {
    super();
    if (player == null) {
      throw new IllegalStateException("no player for capitol:" + location);
    }
    m_player = player.getName();
    m_location = location.getName();
    m_uiContext = uiContext;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    // Changed back to use Large flags
    final Image img = m_uiContext.getFlagImageFactory().getLargeFlag(data.getPlayerList().getPlayerID(m_player));
    final Point point = mapData.getCapitolMarkerLocation(data.getMap().getTerritory(m_location));
    graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public int getLevel() {
    return CAPITOL_MARKER_LEVEL;
  }
}
