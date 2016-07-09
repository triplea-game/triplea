package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.mapdata.MapData;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

// Class to use 'Faded' country markers for Kamikaze Zones.
class KamikazeZoneDrawable implements IDrawable {
  private final String m_location;
  private final IUIContext m_uiContext;

  public KamikazeZoneDrawable(final Territory location, final IUIContext uiContext2) {
    super();
    m_location = location.getName();
    m_uiContext = uiContext2;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    // Change so only original owner gets the kamikazi zone marker
    final Territory terr = data.getMap().getTerritory(m_location);
    final TerritoryAttachment ta = TerritoryAttachment.get(terr);
    PlayerID owner = null;
    if (games.strategy.triplea.Properties.getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(data)) {
      owner = terr.getOwner();
      if (owner == null) {
        owner = PlayerID.NULL_PLAYERID;
      }
    } else {
      if (ta == null) {
        owner = PlayerID.NULL_PLAYERID;
      } else {
        owner = ta.getOriginalOwner();
        if (owner == null) {
          owner = PlayerID.NULL_PLAYERID;
        }
      }
    }
    final Image img = m_uiContext.getFlagImageFactory().getFadedFlag(owner);
    final Point point = mapData.getKamikazeMarkerLocation(data.getMap().getTerritory(m_location));
    graphics.drawImage(img, point.x - bounds.x, point.y - bounds.y, null);
  }

  @Override
  public int getLevel() {
    return CAPITOL_MARKER_LEVEL;
  }
}
