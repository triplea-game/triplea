package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.FogOfWarDrawable;
import games.strategy.triplea.ui.screen.drawable.LandTerritoryDrawable;
import games.strategy.ui.ImageScrollerSmallView;
import games.strategy.ui.Util;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Manages rendering the small map image. The small map image provides a high-level overview of the
 * entire map, including those areas that may not be currently displayed within the large map view.
 */
public class SmallMapImageManager {
  private final ImageScrollerSmallView view;
  private Image offscreen;
  private final TileManager tileManager;

  public SmallMapImageManager(
      final ImageScrollerSmallView view,
      final BufferedImage offscreen,
      final TileManager tileManager) {
    this.view = view;
    this.offscreen = Util.copyImage(offscreen);
    this.tileManager = tileManager;
  }

  public void updateOffscreenImage(final BufferedImage offscreen) {
    this.offscreen.flush();
    this.offscreen = Util.copyImage(offscreen);
  }

  public void update(final MapData mapData) {
    final Graphics onScreenGraphics = view.getOffScreenImage().getGraphics();
    onScreenGraphics.drawImage(offscreen, 0, 0, null);
    final int smallMapUnitSize = mapData.getSmallMapUnitSize();
    final double ratioX = view.getRatioX();
    final double ratioY = view.getRatioY();
    for (final UnitsDrawer drawer : tileManager.getUnitDrawables()) {
      final Point placementPoint = drawer.getPlacementPoint();
      final int x = (int) (placementPoint.x * ratioX);
      final int y = (int) (placementPoint.y * ratioY);
      onScreenGraphics.setColor(mapData.getPlayerColor(drawer.getPlayer()).darker());
      onScreenGraphics.fillRect(x, y, smallMapUnitSize, smallMapUnitSize);
    }
    onScreenGraphics.dispose();
  }

  /** Redraws the specified territory to reflect any change in ownership. */
  public void updateTerritoryOwner(final Territory t, final GameState data, final MapData mapData) {
    if (t.isWater()) {
      return;
    }
    final Rectangle bounds = new Rectangle(mapData.getBoundingRect(t.getName()));
    final double ratioX = view.getRatioX();
    final double ratioY = view.getRatioY();
    final Graphics2D g = (Graphics2D) offscreen.getGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
      g.setRenderingHint(
          RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      // Scale polygon coordinates from full-map space down to the small-map offscreen.
      // LandTerritoryDrawable translates polygons by (-bounds.x, -bounds.y), so pre-translate
      // the graphics so the territory lands at its scaled position.
      g.translate(bounds.x * ratioX, bounds.y * ratioY);
      g.scale(ratioX, ratioY);
      if (tileManager.isTerritoryVisible(t)) {
        new LandTerritoryDrawable(t)
            .draw(bounds, g, mapData, mapData.getSmallMapTerritorySaturation());
      } else {
        new FogOfWarDrawable(t).draw(bounds, g, mapData);
      }
    } finally {
      g.dispose();
    }
  }
}
