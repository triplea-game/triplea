package games.strategy.triplea.ui.screen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.data.GameData;
import games.strategy.thread.LockUtil;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.DrawableComparator;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

public class Tile {
  public static final LockUtil LOCK_UTIL = LockUtil.INSTANCE;
  private static final boolean DRAW_DEBUG = false;
  private static final Logger logger = Logger.getLogger(Tile.class.getName());

  // allow the gc to implement memory management
  private SoftReference<Image> imageRef;
  private boolean isDirty = true;
  private final Rectangle bounds;
  private final int x;
  private final int y;
  private final double scale;
  private final Lock lock = new ReentrantLock();
  private final List<IDrawable> contents = new ArrayList<>();

  Tile(final Rectangle bounds, final int x, final int y, final double scale) {
    this.bounds = bounds;
    this.x = x;
    this.y = y;
    this.scale = scale;
  }

  public boolean isDirty() {
    acquireLock();
    try {
      return isDirty || (imageRef == null) || (imageRef.get() == null);
    } finally {
      releaseLock();
    }
  }

  public void acquireLock() {
    LOCK_UTIL.acquireLock(lock);
  }

  public void releaseLock() {
    LOCK_UTIL.releaseLock(lock);
  }

  public Image getImage(final GameData data, final MapData mapData) {
    acquireLock();
    try {
      if (imageRef == null) {
        imageRef = new SoftReference<>(createBlankImage());
        isDirty = true;
      }
      Image image = imageRef.get();
      if (image == null) {
        image = createBlankImage();
        imageRef = new SoftReference<>(image);
        isDirty = true;
      }
      if (isDirty) {
        final Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        draw(g, data, mapData);
        g.dispose();
      }
      return image;
    } finally {
      releaseLock();
    }
  }

  private BufferedImage createBlankImage() {
    return Util.createImage((int) (bounds.getWidth() * scale), (int) (bounds.getHeight() * scale), false);
  }

  /**
   * This image may be null, and it may not reflect our current drawables. Use getImage() to get
   * a correct image
   *
   * @return the image we currently have.
   */
  public Image getRawImage() {
    if (imageRef == null) {
      return null;
    }
    return imageRef.get();
  }

  private void draw(final Graphics2D g, final GameData data, final MapData mapData) {
    final AffineTransform unscaled = g.getTransform();
    final AffineTransform scaled;
    if (scale != 1) {
      scaled = new AffineTransform();
      scaled.scale(scale, scale);
      g.setTransform(scaled);
    } else {
      scaled = unscaled;
    }
    final Stopwatch stopWatch = new Stopwatch(logger, Level.FINEST, "Drawing Tile at" + bounds);
    // clear
    g.setColor(Color.BLACK);
    g.fill(new Rectangle(0, 0, TileManager.TILE_SIZE, TileManager.TILE_SIZE));
    Collections.sort(contents, new DrawableComparator());
    for (final IDrawable drawable : contents) {
      drawable.draw(bounds, data, g, mapData, unscaled, scaled);
    }
    isDirty = false;
    // draw debug graphics
    if (DRAW_DEBUG) {
      g.setColor(Color.PINK);
      final Rectangle r = new Rectangle(1, 1, TileManager.TILE_SIZE - 2, TileManager.TILE_SIZE - 2);
      g.setStroke(new BasicStroke(1));
      g.draw(r);
      g.setFont(new Font("Ariel", Font.BOLD, 25));
      g.drawString(x + " " + y, 40, 40);
    }
    stopWatch.done();
  }

  void addDrawables(final Collection<IDrawable> drawables) {
    acquireLock();
    try {
      contents.addAll(drawables);
      isDirty = true;
    } finally {
      releaseLock();
    }
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
