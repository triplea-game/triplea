package games.strategy.triplea.ui.screen.drawable;

import java.awt.Image;

import games.strategy.triplea.ui.UiContext;

public class BaseMapDrawable extends MapTileDrawable {
  public BaseMapDrawable(final int x, final int y, final UiContext uiContext) {
    super(x, y, uiContext);
  }

  @Override
  public MapTileDrawable getUnscaledCopy() {
    final BaseMapDrawable copy = new BaseMapDrawable(x, y, uiContext);
    copy.unscaled = true;
    return copy;
  }

  @Override
  protected Image getImage() {
    if (noImage) {
      return null;
    }
    final Image image;
    if (unscaled) {
      image = uiContext.getTileImageFactory().getUnscaledUncachedBaseTile(x, y);
    } else {
      image = uiContext.getTileImageFactory().getBaseTile(x, y);
    }
    if (image == null) {
      noImage = true;
    }
    return image;
  }

  @Override
  public int getLevel() {
    return BASE_MAP_LEVEL;
  }
}
