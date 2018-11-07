package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;

/**
 * Draws the faded flag image of the original owner for the associated territory. Intended only for use with kamikaze
 * zones.
 */
public class KamikazeZoneDrawable implements IDrawable {
  private final String location;
  private final UiContext uiContext;

  public KamikazeZoneDrawable(final Territory location, final UiContext uiContext) {
    this.location = location.getName();
    this.uiContext = uiContext;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    // Change so only original owner gets the kamikazi zone marker
    final Territory terr = data.getMap().getTerritory(location);
    final TerritoryAttachment ta = TerritoryAttachment.get(terr);
    PlayerId owner;
    if (Properties.getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(data)) {
      owner = terr.getOwner();
      if (owner == null) {
        owner = PlayerId.NULL_PLAYERID;
      }
    } else {
      if (ta == null) {
        owner = PlayerId.NULL_PLAYERID;
      } else {
        owner = ta.getOriginalOwner();
        if (owner == null) {
          owner = PlayerId.NULL_PLAYERID;
        }
      }
    }
    final Image img = uiContext.getFlagImageFactory().getFadedFlag(owner);
    final Point point = mapData.getKamikazeMarkerLocation(data.getMap().getTerritory(location));
    graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public int getLevel() {
    return CAPITOL_MARKER_LEVEL;
  }
}
