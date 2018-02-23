package games.strategy.triplea.ui.screen.drawable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.ui.mapdata.MapData;

public class LandTerritoryDrawable extends TerritoryDrawable implements IDrawable {
  private final String territoryName;

  public LandTerritoryDrawable(final String territoryName) {
    this.territoryName = territoryName;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    final Territory territory = data.getMap().getTerritory(territoryName);
    final Color territoryColor;
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    if ((ta != null) && ta.getIsImpassable()) {
      territoryColor = mapData.impassableColor();
    } else {
      territoryColor = mapData.getPlayerColor(territory.getOwner().getName());
    }
    draw(bounds, graphics, mapData, territory, territoryColor);
  }

  @Override
  public int getLevel() {
    return POLYGONS_LEVEL;
  }
}
