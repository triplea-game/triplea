package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.TileManager;

public abstract class MapTileDrawable implements IDrawable {
  protected boolean noImage = false;
  protected final int x;
  protected final int y;
  protected final UiContext uiContext;
  protected boolean unscaled;

  protected MapTileDrawable(final int x, final int y, final UiContext uiContext) {
    this.x = x;
    this.y = y;
    this.uiContext = uiContext;
    unscaled = false;
  }

  public abstract MapTileDrawable getUnscaledCopy();

  protected abstract Image getImage();

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData,
      final AffineTransform unscaled, final AffineTransform scaled) {
    final Image img = getImage();
    if (img == null) {
      return;
    }
    final Object oldRenderingValue = graphics.getRenderingHint(RenderingHints.KEY_RENDERING);
    final Object oldAlphaValue = graphics.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
    final Object oldInterpolationValue = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    // the tile images are already scaled
    if (unscaled != null) {
      graphics.setTransform(unscaled);
    }
    graphics.drawImage(img, (x * TileManager.TILE_SIZE) - bounds.x, (y * TileManager.TILE_SIZE) - bounds.y, null);
    if (unscaled != null) {
      graphics.setTransform(scaled);
    }
    if (oldAlphaValue == null) {
      graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
          RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
    } else {
      graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldAlphaValue);
    }
    if (oldRenderingValue == null) {
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
    } else {
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, oldRenderingValue);
    }
    if (oldInterpolationValue == null) {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    } else {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolationValue);
    }
  }
}
