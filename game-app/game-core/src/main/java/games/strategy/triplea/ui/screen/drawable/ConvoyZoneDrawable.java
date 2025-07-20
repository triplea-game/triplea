package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

/** Draws the convoy flag image for the associated territory. */
public class ConvoyZoneDrawable extends AbstractDrawable {
  private final String player;
  private final String location;
  private final UiContext uiContext;

  public ConvoyZoneDrawable(
      final GamePlayer player, final Territory location, final UiContext uiContext) {
    this.player = player.getName();
    this.location = location.getName();
    this.uiContext = uiContext;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    final Image img;
    if (mapData.useNationConvoyFlags()) {
      img = uiContext.getFlagImageFactory().getConvoyFlag(data.getPlayerList().getPlayerId(player));
    } else {
      img = uiContext.getFlagImageFactory().getFlag(data.getPlayerList().getPlayerId(player));
    }
    final Point point =
        mapData.getConvoyMarkerLocation(data.getMap().getTerritoryOrThrow(location));
    graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.CAPITOL_MARKER_LEVEL;
  }
}
