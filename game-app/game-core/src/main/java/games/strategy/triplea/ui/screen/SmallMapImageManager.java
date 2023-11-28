package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.LandTerritoryDrawable;
import games.strategy.ui.ImageScrollerSmallView;
import games.strategy.ui.Util;
import java.awt.AlphaComposite;
import java.awt.Color;
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
    // create a large image for the territory
    final Rectangle bounds = new Rectangle(mapData.getBoundingRect(t.getName()));
    final Image largeImage = createLargeImage(t, bounds, mapData);

    // scale it down
    final double ratioX = view.getRatioX();
    final double ratioY = view.getRatioY();
    int thumbWidth = (int) (bounds.width * ratioX);
    int thumbHeight = (int) (bounds.height * ratioY);
    // the images won't overlap perfectly after being scaled, make them a little bigger to
    // re-balance that
    thumbWidth += 3;
    thumbHeight += 3;
    final int thumbsX = (int) (bounds.x * ratioX) - 1;
    final int thumbsY = (int) (bounds.y * ratioY) - 1;
    // create the thumb image
    final Image thumbImage = Util.newImage(thumbWidth, thumbHeight, true);
    {
      final Graphics g = thumbImage.getGraphics();
      g.drawImage(largeImage, 0, 0, thumbWidth, thumbHeight, null);
      g.dispose();
    }
    {
      final Graphics g = offscreen.getGraphics();
      // draw it on our offscreen
      g.drawImage(thumbImage, thumbsX, thumbsY, thumbWidth, thumbHeight, null);
      g.dispose();
    }
  }

  private Image createLargeImage(Territory t, Rectangle bounds, MapData mapData) {
    // create a large image for the territory
    final Image largeImage = Util.newImage(bounds.width, bounds.height, true);
    // make it transparent
    // http://www-106.ibm.com/developerworks/library/j-begjava/
    // Once the image is transparent, we will be drawing the territory shape and filling
    // the territory polygon. Any remaining edges will be transparent.
    {
      final Graphics2D g = (Graphics2D) largeImage.getGraphics();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, bounds.width, bounds.height);
      g.dispose();
    }
    // draw the territory
    {
      final Graphics2D g = (Graphics2D) largeImage.getGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
      g.setRenderingHint(
          RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      final LandTerritoryDrawable drawable = new LandTerritoryDrawable(t);
      drawable.draw(bounds, g, mapData, mapData.getSmallMapTerritorySaturation());
      g.dispose();
    }
    return largeImage;
  }
}
