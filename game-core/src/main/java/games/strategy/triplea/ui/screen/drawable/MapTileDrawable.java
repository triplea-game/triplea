package games.strategy.triplea.ui.screen.drawable;

import static games.strategy.triplea.ui.screen.TileManager.TILE_SIZE;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.mapdata.MapData;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Superclass for {@link IDrawable} implementations that draws a single rectangular map tile. */
public abstract class MapTileDrawable extends AbstractDrawable {
  protected boolean noImage = false;

  @SuppressWarnings("checkstyle:MemberName")
  protected final int x;

  @SuppressWarnings("checkstyle:MemberName")
  protected final int y;

  protected final UiContext uiContext;

  protected MapTileDrawable(final int x, final int y, final UiContext uiContext) {
    this.x = x;
    this.y = y;
    this.uiContext = uiContext;
  }

  protected abstract Image getImage();

  @Override
  public void draw(
      final Rectangle bounds,
      final GameData data,
      final Graphics2D graphics,
      final MapData mapData) {
    final Image img = getImage();
    if (img == null) {
      return;
    }
    final Object oldRenderingValue = graphics.getRenderingHint(RenderingHints.KEY_RENDERING);
    final Object oldAlphaValue = graphics.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
    final Object oldInterpolationValue =
        graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    graphics.setRenderingHint(
        RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

    graphics.drawImage(img, x * TILE_SIZE - bounds.x, y * TILE_SIZE - bounds.y, null);
    if (oldAlphaValue == null) {
      graphics.setRenderingHint(
          RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_DEFAULT);
    } else {
      graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldAlphaValue);
    }
    if (oldRenderingValue == null) {
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
    } else {
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, oldRenderingValue);
    }
    if (oldInterpolationValue == null) {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    } else {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolationValue);
    }
  }
}
