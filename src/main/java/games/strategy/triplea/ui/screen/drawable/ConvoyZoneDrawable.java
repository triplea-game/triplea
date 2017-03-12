package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.mapdata.MapData;

// Rewritten class to use country markers rather than shading for Convoy Centers/Routes.
public class ConvoyZoneDrawable implements IDrawable {
  private final String m_player;
  private final String m_location;
  private final IUIContext m_uiContext;

  public ConvoyZoneDrawable(final PlayerID player, final Territory location, final IUIContext uiContext) {
    super();
    m_player = player.getName();
    m_location = location.getName();
    m_uiContext = uiContext;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    Image img;
    if (mapData.useNation_convoyFlags()) {
      img = m_uiContext.getFlagImageFactory().getConvoyFlag(data.getPlayerList().getPlayerID(m_player));
    } else {
      img = m_uiContext.getFlagImageFactory().getFlag(data.getPlayerList().getPlayerID(m_player));
    }
    final Point point = mapData.getConvoyMarkerLocation(data.getMap().getTerritory(m_location));
    graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public int getLevel() {
    return CAPITOL_MARKER_LEVEL;
  }
}
