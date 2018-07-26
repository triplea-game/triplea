package games.strategy.triplea.ui.screen.drawable;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

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
    graphics.drawImage(img, x * TileManager.TILE_SIZE - bounds.x, y * TileManager.TILE_SIZE - bounds.y, null);
  }
}
