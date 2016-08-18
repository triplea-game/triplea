package games.strategy.triplea.ui.screen.drawable;

import java.awt.Image;

import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.IUIContext;

public class ReliefMapDrawable extends MapTileDrawable {
  public ReliefMapDrawable(final int x, final int y, final IUIContext context) {
    super(x, y, context);
  }

  @Override
  public MapTileDrawable getUnscaledCopy() {
    final ReliefMapDrawable copy = new ReliefMapDrawable(m_x, m_y, m_uiContext);
    copy.m_unscaled = true;
    return copy;
  }

  @Override
  protected Image getImage() {
    if (m_noImage) {
      return null;
    }
    if (!TileImageFactory.getShowReliefImages()) {
      return null;
    }
    Image rVal;
    if (m_unscaled) {
      rVal = m_uiContext.getTileImageFactory().getUnscaledUncachedReliefTile(m_x, m_y);
    } else {
      rVal = m_uiContext.getTileImageFactory().getReliefTile(m_x, m_y);
    }
    if (rVal == null) {
      m_noImage = true;
    }
    return rVal;
  }

  @Override
  public int getLevel() {
    return RELIEF_LEVEL;
  }
}
