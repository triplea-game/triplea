package games.strategy.triplea.ui.screen.drawable;

import games.strategy.triplea.ui.UiContext;
import java.awt.Image;

/** Draws a base map tile. */
public class BaseMapDrawable extends MapTileDrawable {
  public BaseMapDrawable(final int x, final int y, final UiContext uiContext) {
    super(x, y, uiContext);
  }

  @Override
  protected Image getImage() {
    if (noImage) {
      return null;
    }
    final Image image = uiContext.getTileImageFactory().getBaseTile(x, y);
    if (image == null) {
      noImage = true;
    }
    return image;
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.BASE_MAP_LEVEL;
  }
}
