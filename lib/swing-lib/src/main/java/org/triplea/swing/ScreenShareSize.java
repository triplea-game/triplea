package org.triplea.swing;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import lombok.experimental.UtilityClass;

/**
 * Sizes windows as a share of the screen, so they open usefully large on big monitors yet never
 * smaller than their content nor larger than the screen.
 */
@UtilityClass
public class ScreenShareSize {

  /**
   * Resizes {@code window} to the given shares of the usable screen, keeping its current size as
   * the minimum, and centres it over its owner. Call after the window's content is in place.
   */
  public static void apply(final Window window, final double widthShare, final double heightShare) {
    window.setSize(
        compute(
            window.getSize(),
            GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds(),
            widthShare,
            heightShare));
    window.setLocationRelativeTo(window.getOwner());
  }

  /**
   * Returns the given shares of {@code usableScreen}, raised to at least {@code minimum}, then
   * capped to fit on the screen.
   */
  public static Dimension compute(
      final Dimension minimum,
      final Rectangle usableScreen,
      final double widthShare,
      final double heightShare) {
    return fitToScreen(
        new Dimension(
            Math.max(minimum.width, (int) (usableScreen.width * widthShare)),
            Math.max(minimum.height, (int) (usableScreen.height * heightShare))),
        usableScreen);
  }

  /** Returns {@code size} shrunk where needed to fit on {@code usableScreen}. */
  public static Dimension fitToScreen(final Dimension size, final Rectangle usableScreen) {
    return new Dimension(
        Math.min(size.width, usableScreen.width), Math.min(size.height, usableScreen.height));
  }
}
