package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;

public class CapitolMarkerDrawable implements IDrawable {
  private final String player;
  private final String location;
  private final UiContext uiContext;

  public CapitolMarkerDrawable(final PlayerID player, final Territory location, final UiContext uiContext) {
    super();
    if (player == null) {
      throw new IllegalStateException("no player for capitol:" + location);
    }
    this.player = player.getName();
    this.location = location.getName();
    this.uiContext = uiContext;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    // Changed back to use Large flags
    final Image img = uiContext.getFlagImageFactory().getLargeFlag(data.getPlayerList().getPlayerId(player));
    final Point point = mapData.getCapitolMarkerLocation(data.getMap().getTerritory(location));
    graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public int getLevel() {
    return CAPITOL_MARKER_LEVEL;
  }
}
