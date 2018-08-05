package games.strategy.triplea.ui.screen;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

/**
 * A class representing a Tile of the Map and storing the image to be rendered
 * on the screen.
 * This class doesn't guarantee to be Thread-Safe although
 * it's safe to use this class between multiple Threads
 * which might cause outdated Images which is totally fine.
 */
public class Tile {
  private volatile boolean isDirty = true;

  private final Image image;
  private final Rectangle bounds;
  private final Queue<IDrawable> contents = new PriorityBlockingQueue<>(1,
      Comparator.comparingInt(IDrawable::getLevel));

  Tile(final Rectangle bounds) {
    this(bounds, Util.createImage((int) bounds.getWidth(), (int) bounds.getHeight(), true));
  }

  @VisibleForTesting
  Tile(final Rectangle bounds, final Image image) {
    this.bounds = bounds;
    this.image = image;
  }

  public boolean isDirty() {
    return isDirty;
  }

  public Image getImage() {
    return image;
  }

  /**
   * This method draws an image based on the provided GameData and MapData.
   * It first creates an empty image with the same size and properties
   * as the image field, applies all of the drawing operations to it
   * copies the resulting pixels to the stored image and disposes
   * the resources afterwards.
   *
   * <p>
   * This is to ensure we don't draw the Tile mid-generating
   * without having to synchronize it.
   * </p>
   */
  public void drawImage(final GameData data, final MapData mapData) {
    final BufferedImage writeBuffer = Util.createImage(image.getWidth(null),
        image.getHeight(null), true);
    final Graphics2D g = (Graphics2D) writeBuffer.getGraphics();
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    draw(g, data, mapData);

    final Graphics2D imageGraphics = (Graphics2D) image.getGraphics();
    imageGraphics.drawImage(writeBuffer, new AffineTransform(), null);
    imageGraphics.dispose();
    g.dispose();
  }

  private void draw(final Graphics2D g, final GameData data, final MapData mapData) {
    final Stopwatch stopWatch = new Stopwatch("Drawing Tile at " + bounds);
    final Queue<IDrawable> clone = new PriorityBlockingQueue<>(contents);
    while (!clone.isEmpty()) {
      clone.remove().draw(bounds, data, g, mapData);
    }
    isDirty = false;
    stopWatch.done();
  }

  void addDrawables(final Collection<IDrawable> drawables) {
    contents.addAll(drawables);
    isDirty = true;
  }

  void addDrawable(final IDrawable d) {
    contents.add(d);
    isDirty = true;
  }

  void removeDrawables(final Collection<IDrawable> c) {
    contents.removeAll(c);
    isDirty = true;
  }

  void clear() {
    contents.clear();
    isDirty = true;
  }

  List<IDrawable> getDrawables() {
    return new ArrayList<>(contents);
  }

  public Rectangle getBounds() {
    return bounds;
  }
}
