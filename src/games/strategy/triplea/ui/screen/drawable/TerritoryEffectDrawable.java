package games.strategy.triplea.ui.screen.drawable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.ui.mapdata.MapData;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

class TerritoryEffectDrawable implements IDrawable {
  private final TerritoryEffect m_effect;
  private final Point m_point;

  public TerritoryEffectDrawable(final TerritoryEffect te, final Point point) {
    super();
    m_effect = te;
    m_point = point;
  }

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    graphics.drawImage(mapData.getTerritoryEffectImage(m_effect.getName()), m_point.x - bounds.x, m_point.y - bounds.y,
        null);
  }

  @Override
  public int getLevel() {
    return TERRITORY_EFFECT_LEVEL;
  }
}
