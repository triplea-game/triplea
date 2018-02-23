package games.strategy.ui;

import java.awt.Dimension;
import java.util.Observable;

/**
 * Model for an ImageScroller. Generally one large view and one small view will be
 * connected to the same model.
 *
 * <p>
 * notifies its observers when changes occur.
 * </p>
 */
public class ImageScrollModel extends Observable {
  private int x;
  private int y;
  private int boxWidth = 5;
  private int boxHeight = 5;
  private int maxWidth;
  private int maxHeight;
  private boolean scrollX;
  private boolean scrollY;

  public void setMaxBounds(final int maxWidth, final int maxHeight) {
    this.maxWidth = maxWidth;
    this.maxHeight = maxHeight;
    enforceBounds();
    updateListeners();
  }

  public void setMaxBounds(final Dimension mapDimensions) {
    setMaxBounds(mapDimensions.width, mapDimensions.height);
  }

  void setBoxDimensions(final int maxX, final int maxy) {
    boxWidth = maxX;
    boxHeight = maxy;
    enforceBounds();
    updateListeners();
  }

  private void updateListeners() {
    super.setChanged();
    super.notifyObservers();
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
      if ((y + boxHeight) > maxHeight) {
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
    // if the box is bigger than the map
    // put us at 0,0
    if (boxHeight > maxHeight) {
      y = 0;
    }
    if (!scrollX) {
      if (x < 0) {
        x = 0;
      }
      if ((x + boxWidth) > maxWidth) {
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
    // if the box is bigger than the map
    // put us at 0,0
    if (boxWidth > maxWidth) {
      x = 0;
    }
  }

  public boolean getScrollX() {
    return scrollX;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getBoxWidth() {
    return boxWidth;
  }

  public int getBoxHeight() {
    return boxHeight;
  }

  public int getMaxWidth() {
    return maxWidth;
  }

  public int getMaxHeight() {
    return maxHeight;
  }

  void set(final int x, final int y) {
    this.x = x;
    this.y = y;
    enforceBounds();
    updateListeners();
  }
}
