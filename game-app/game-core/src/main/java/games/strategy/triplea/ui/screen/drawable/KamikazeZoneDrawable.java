package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Optional;

/**
 * Draws the faded flag image of the original owner for the associated territory. Intended only for
 * use with kamikaze zones.
 */
public class KamikazeZoneDrawable extends AbstractDrawable {
  private final String location;
  private final UiContext uiContext;

  public KamikazeZoneDrawable(final Territory location, final UiContext uiContext) {
    this.location = location.getName();
    this.uiContext = uiContext;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    // Change so only original owner gets the kamikaze zone marker
    final Territory terr = data.getMap().getTerritoryOrThrow(location);
    GamePlayer owner;
    if (Properties.getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(data.getProperties())) {
      owner = terr.getOwner();
      if (owner == null) {
        owner = data.getPlayerList().getNullPlayer();
      }
    } else {
      final Optional<TerritoryAttachment> optionalTerritoryAttachment =
          TerritoryAttachment.get(terr);
      if (optionalTerritoryAttachment.isEmpty()) {
        owner = data.getPlayerList().getNullPlayer();
      } else {
        owner =
            optionalTerritoryAttachment
                .get()
                .getOriginalOwner()
                .orElse(data.getPlayerList().getNullPlayer());
      }
    }
    final Image img = uiContext.getFlagImageFactory().getFadedFlag(owner);
    final Point point =
        mapData.getKamikazeMarkerLocation(data.getMap().getTerritoryOrThrow(location));
    graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.CAPITOL_MARKER_LEVEL;
  }
}
