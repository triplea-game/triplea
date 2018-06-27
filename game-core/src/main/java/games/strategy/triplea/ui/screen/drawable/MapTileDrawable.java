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

  protected MapTileDrawable(final int x, final int y, final UiContext uiContext) {
    this.x = x;
    this.y = y;
    this.uiContext = uiContext;
  }

  public abstract MapTileDrawable getUnscaledCopy();

  protected abstract Image getImage();

  @Override
  public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData) {
    final Image img = getImage();
    if (img == null) {
      return;
    }
    final RenderingHints hints = graphics.getRenderingHints();
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    graphics.drawImage(img, x * TileManager.TILE_SIZE - bounds.x, y * TileManager.TILE_SIZE - bounds.y, null);
    graphics.setRenderingHints(hints);
  }
}
