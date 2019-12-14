package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.ui.Util;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.triplea.thread.LockUtil;

/** Responsible for rendering a single map tile. */
public class Tile {
  private boolean isDirty = true;

  private final Image image;
  private final Rectangle bounds;
  private final Lock lock = new ReentrantLock();
  private final Queue<IDrawable> contents = new PriorityQueue<>();

  Tile(final Rectangle bounds) {
    this.bounds = bounds;
    image = Util.newImage((int) bounds.getWidth(), (int) bounds.getHeight(), true);
  }

  public boolean isDirty() {
    acquireLock();
    try {
      return isDirty;
    } finally {
      releaseLock();
    }
  }

  public void acquireLock() {
    LockUtil.INSTANCE.acquireLock(lock);
  }

  public void releaseLock() {
    LockUtil.INSTANCE.releaseLock(lock);
  }

  /** Returns the image representing this tile, re-rendering it first if the tile is dirty. */
  public Image getImage(final GameData data, final MapData mapData) {
    acquireLock();
    try {
      if (isDirty) {
        final Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(
            RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        draw(g, data, mapData);
        g.dispose();
      }
      return image;
    } finally {
      releaseLock();
    }
  }

  /**
   * This image may not reflect our current drawables. Use getImage() to get a correct image
   *
   * @return the image we currently have.
   */
  public Image getRawImage() {
    return image;
  }

  private void draw(final Graphics2D g, final GameData data, final MapData mapData) {
    final AffineTransform original = g.getTransform();
    // clear
    g.setColor(Color.BLACK);
    g.fill(new Rectangle(0, 0, TileManager.TILE_SIZE, TileManager.TILE_SIZE));
    final Queue<IDrawable> queue = new PriorityQueue<>(contents);
    while (!queue.isEmpty()) {
      queue.remove().draw(bounds, data, g, mapData);
      // Make sure we don't mess up other draws
      g.setTransform(original);
    }
    isDirty = false;
  }

  void addDrawables(final Collection<IDrawable> drawables) {
    drawables.forEach(this::addDrawable);
  }

  void addDrawable(final IDrawable d) {
    acquireLock();
    try {
      contents.add(d);
      isDirty = true;
    } finally {
      releaseLock();
    }
  }

  void removeDrawables(final Collection<IDrawable> c) {
    acquireLock();
    try {
      contents.removeAll(c);
      isDirty = true;
    } finally {
      releaseLock();
    }
  }

  void clear() {
    acquireLock();
    try {
      contents.clear();
      isDirty = true;
    } finally {
      releaseLock();
    }
  }

  List<IDrawable> getDrawables() {
    acquireLock();
    try {
      return new ArrayList<>(contents);
    } finally {
      releaseLock();
    }
  }

  public Rectangle getBounds() {
    return bounds;
  }
}
