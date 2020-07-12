package games.strategy.triplea.ui.screen.drawable;

import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.UiContext;

import java.awt.Image;

/** Draws a relief map tile. */
public class ReliefMapDrawable extends MapTileDrawable {
  public ReliefMapDrawable(final int x, final int y, final UiContext uiContext) {
    super(x, y, uiContext);
  }

  @Override
  protected Image getImage() {
    if (noImage) {
      return null;
    }
    if (!TileImageFactory.getShowReliefImages()) {
      return null;
    }
    final Image image = uiContext.getTileImageFactory().getReliefTile(x, y);
    if (image == null) {
      noImage = true;
    }
    return image;
  }

  @Override
  public DrawLevel getLevel() {
    return DrawLevel.RELIEF_LEVEL;
  }
}
