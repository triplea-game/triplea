package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/** Draws a territory effect image at a given point on the map. */
public class TerritoryEffectDrawable extends AbstractDrawable {
  private final TerritoryEffect effect;
  private final Point point;

  public TerritoryEffectDrawable(final TerritoryEffect te, final Point point) {
    effect = te;
    this.point = point;
  }

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    drawImage(graphics, mapData.getTerritoryEffectImage(effect.getName()), point, bounds);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.TERRITORY_EFFECT_LEVEL;
  }
}
