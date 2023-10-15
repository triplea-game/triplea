package org.triplea.swing.gestures;

/** Listener for magnification events (e.g. pinch gesture with a trackpad). */
public interface MagnificationListener {
  /**
   * Called when a magnification event occurred.
   *
   * @param factor The magnification multiplier to apply over the current zoom level (e.g. 1 = no
   *     change, 2 = double the zoom, etc).
   */
  void magnify(double factor);
}
