package games.strategy.triplea.ui.screen.drawable;

import games.strategy.triplea.ui.IUIContext;

import java.awt.Image;

class BaseMapDrawable extends MapTileDrawable {
  public BaseMapDrawable(final int x, final int y, final IUIContext uiContext) {
    super(x, y, uiContext);
  }

  @Override
  public MapTileDrawable getUnscaledCopy() {
    final BaseMapDrawable copy = new BaseMapDrawable(m_x, m_y, m_uiContext);
    copy.m_unscaled = true;
    return copy;
  }

  @Override
  protected Image getImage() {
    if (m_noImage) {
      return null;
    }
    Image rVal;
    if (m_unscaled) {
      rVal = m_uiContext.getTileImageFactory().getUnscaledUncachedBaseTile(m_x, m_y);
    } else {
      rVal = m_uiContext.getTileImageFactory().getBaseTile(m_x, m_y);
    }
    if (rVal == null) {
      m_noImage = true;
    }
    return rVal;
  }

  @Override
  public int getLevel() {
    return BASE_MAP_LEVEL;
  }
}
