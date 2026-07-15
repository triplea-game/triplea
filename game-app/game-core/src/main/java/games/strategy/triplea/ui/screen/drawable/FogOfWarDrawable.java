package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import lombok.AllArgsConstructor;

/** Covers a hidden territory while preserving its public shape and name. */
@AllArgsConstructor
public final class FogOfWarDrawable extends TerritoryDrawable {
  private static final Color FOG_COLOR = new Color(24, 28, 32);

  private final Territory territory;

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    draw(bounds, graphics, mapData);
  }

  public void draw(final Rectangle bounds, final Graphics2D graphics, final MapData mapData) {
    draw(bounds, graphics, mapData, territory, FOG_COLOR);
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.DECORATOR_LEVEL;
  }
}
