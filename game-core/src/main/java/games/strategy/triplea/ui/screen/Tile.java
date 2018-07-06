package games.strategy.triplea.ui.screen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import games.strategy.engine.data.GameData;
import games.strategy.thread.LockUtil;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable;
import games.strategy.triplea.util.Stopwatch;
import games.strategy.ui.Util;

public class Tile {
  private volatile boolean isDirty = true;
  private volatile boolean drawingStarted = false;

  private final Image image;
  private final Rectangle bounds;
  private final Lock lock = new ReentrantLock();
  private final SortedMap<Integer, List<IDrawable>> contents = new TreeMap<>();

  Tile(final Rectangle bounds) {
    this.bounds = bounds;
    image = Util.createImage((int) bounds.getWidth(), (int) bounds.getHeight(), true);
  }

  public boolean isDirty() {
    return isDirty;
  }

  public boolean hasDrawingStarted() {
    return drawingStarted;
  }

  private void acquireLock() {
    LockUtil.INSTANCE.acquireLock(lock);
  }

  private void releaseLock() {
    LockUtil.INSTANCE.releaseLock(lock);
  }

  public Image getImage(final GameData data, final MapData mapData) {
    if (isDirty) {
      try {
        acquireLock();
        drawingStarted = true;
        final Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        draw(g, data, mapData);
        g.dispose();
      } finally {
        drawingStarted = false;
        releaseLock();
      }
    }
    return image;
  }

  private void draw(final Graphics2D g, final GameData data, final MapData mapData) {
    final Stopwatch stopWatch = new Stopwatch(Logger.getLogger(Tile.class.getName()), Level.FINEST,
        "Drawing Tile at" + bounds);
    for (final List<IDrawable> list : contents.values()) {
      for (final IDrawable drawable : list) {
        drawable.draw(bounds, data, g, mapData);
      }
    }
    isDirty = false;
    stopWatch.done();
  }

  void addDrawables(final Collection<IDrawable> drawables) {
    drawables.forEach(this::addDrawable);
  }

  void addDrawable(final IDrawable d) {
    acquireLock();
    try {
      contents.computeIfAbsent(d.getLevel(), l -> new ArrayList<>()).add(d);
      isDirty = true;
    } finally {
      releaseLock();
    }
  }

  void removeDrawables(final Collection<IDrawable> c) {
    acquireLock();
    try {
      contents.values().forEach(l -> l.removeAll(c));
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
      return contents.values().stream()
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    } finally {
      releaseLock();
    }
  }

  public Rectangle getBounds() {
    return bounds;
  }
}
