package games.strategy.ui;

import java.util.ArrayList;
import java.util.Collection;
import lombok.Getter;

/**
 * Model for an ImageScroller. Generally one large view and one small view will be connected to the
 * same model.
 *
 * <p>notifies its observers when changes occur.
 */
public class ImageScrollModel {
  @Getter private int x;

  @Getter private int y;

  @Getter private int boxWidth = 5;
  @Getter private int boxHeight = 5;
  @Getter private int maxWidth;
  @Getter private int maxHeight;
  private boolean scrollX;
  private boolean scrollY;

  private final Collection<Runnable> listeners = new ArrayList<>();

  public void setMaxBounds(final int maxWidth, final int maxHeight) {
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
    enforceBounds();
    updateListeners();
  }

  void setBoxDimensions(final int maxX, final int maxy) {
    boxWidth = maxX;
    boxHeight = maxy;
    enforceBounds();
    updateListeners();
  }

  public void addListener(final Runnable runnable) {
    listeners.add(runnable);
  }

  private void updateListeners() {
    listeners.forEach(Runnable::run);
  }

  public void setScrollX(final boolean scrollX) {
    this.scrollX = scrollX;
    enforceBounds();
    updateListeners();
  }

  public void setScrollY(final boolean scrollY) {
    this.scrollY = scrollY;
    enforceBounds();
    updateListeners();
  }

  private void enforceBounds() {
    if (!scrollY) {
      if (y < 0) {
        y = 0;
      }
      if (y + boxHeight > maxHeight) {
        y = maxHeight - boxHeight;
      }
    } else {
      // don't let the map scroll infinitely,
      // when it gets to be twice the height to the up or down, move it back one length
      while (y > maxHeight) {
        y -= maxHeight;
      }
      while (y < -maxHeight) {
        y += maxHeight;
      }
    }
    if (!scrollX) {
      if (x < 0) {
        x = 0;
      }
      if (x + boxWidth > maxWidth) {
        x = maxWidth - boxWidth;
      }
    } else {
      // don't let the map scroll infinitely,
      // when it gets to be twice the length to the left or right, move it back one length
      while (x > maxWidth) {
        x -= maxWidth;
      }
      while (x < -maxWidth) {
        x += maxWidth;
      }
    }
  }

  public boolean getScrollX() {
    return scrollX;
  }

  public boolean getScrollY() {
    return scrollY;
  }

  void set(final int x, final int y) {
    this.x = x;
    this.y = y;
    enforceBounds();
    updateListeners();
  }
}
