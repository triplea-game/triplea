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
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;

/** Responsible for rendering a single map tile. */
public class Tile {
  private volatile boolean isDirty = true;
  private final AtomicBoolean isDrawing = new AtomicBoolean(false);

  /** Current de facto immutable state of this tile. */
  @Getter private Image image;

  private final Rectangle bounds;
  private final Object mutex = new Object();
  private final Queue<IDrawable> contents = new PriorityQueue<>();

  Tile(final Rectangle bounds) {
    this.bounds = bounds;
    this.image = Util.newImage(bounds.width, bounds.height, true);
  }

  public boolean needsRedraw() {
    return isDirty && !isDrawing.get();
  }

  /** Returns the image representing this tile, re-rendering it first if the tile is dirty. */
  public void drawImage(final GameData data, final MapData mapData) {
    if (isDirty && !isDrawing.getAndSet(true)) {
      final Image backImage = Util.newImage(bounds.width, bounds.height, true);
      final Graphics2D g = (Graphics2D) backImage.getGraphics();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g.setRenderingHint(
          RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
      g.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      draw(g, data, mapData);
      g.dispose();
      image = backImage;
      isDrawing.set(false);
    }
  }

  private void draw(final Graphics2D g, final GameData data, final MapData mapData) {
    final AffineTransform original = g.getTransform();
    // clear
    g.setColor(Color.BLACK);
    g.fill(new Rectangle(0, 0, bounds.width, bounds.height));
    synchronized (mutex) {
      final Queue<IDrawable> queue = new PriorityQueue<>(contents);
      while (!queue.isEmpty()) {
        queue.remove().draw(bounds, data, g, mapData);
        // Make sure we don't mess up other draws
        g.setTransform(original);
      }
      isDirty = false;
    }
  }

  void addDrawables(final Collection<IDrawable> drawables) {
    drawables.forEach(this::addDrawable);
  }

  void addDrawable(final IDrawable d) {
    synchronized (mutex) {
      contents.add(d);
      isDirty = true;
    }
  }

  void removeDrawables(final Collection<IDrawable> c) {
    synchronized (mutex) {
      contents.removeAll(c);
      isDirty = true;
    }
  }

  void clear() {
    synchronized (mutex) {
      contents.clear();
      isDirty = true;
    }
  }

  List<IDrawable> getDrawables() {
    synchronized (mutex) {
      return new ArrayList<>(contents);
    }
  }

  public Rectangle getBounds() {
    return bounds;
  }
}
