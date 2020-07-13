package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Draws a black outline around the associated territory and draws a solid color over the territory
 * interior. The color is based on the territory owner and whether or not the territory is
 * impassable. Intended only for use with land territories.
 */
public class LandTerritoryDrawable extends TerritoryDrawable {
  private final String territoryName;

  public LandTerritoryDrawable(final String territoryName) {
    this.territoryName = territoryName;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    draw(bounds, data, graphics, mapData, 1.0f);
  }

  /** Determine territory color and set saturation to then draw the territory. */
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData,
      final float saturation) {
    final Territory territory = data.getMap().getTerritory(territoryName);
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);

    final Color territoryColor =
        (ta != null && ta.getIsImpassable())
            ? mapData.impassableColor()
            : mapData.getPlayerColor(territory.getOwner().getName());

    final float[] values =
        Color.RGBtoHSB(
            territoryColor.getRed(), territoryColor.getGreen(), territoryColor.getBlue(), null);
    values[1] = values[1] * saturation;

    final Color territoryHsbColor = Color.getHSBColor(values[0], values[1], values[2]);
    draw(bounds, graphics, mapData, territory, territoryHsbColor);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.POLYGONS_LEVEL;
  }
}
