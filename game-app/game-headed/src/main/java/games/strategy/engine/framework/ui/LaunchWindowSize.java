package games.strategy.engine.framework.ui;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.triplea.swing.ScreenShareSize;

/** Picks the size the launch window opens at. */
@UtilityClass
class LaunchWindowSize {
  /** Share of the usable screen a first-time launch window covers. */
  private static final double SCREEN_SHARE = 0.85;

  /**
   * Returns the size last saved by the user if there is one, otherwise a large share of the screen
   * that is never smaller than {@code minimum}; either way the result fits on the screen.
   */
  static Dimension compute(
      final Optional<Dimension> saved, final Rectangle usableScreen, final Dimension minimum) {
    return saved
        .map(size -> ScreenShareSize.fitToScreen(size, usableScreen))
        .orElseGet(
            () -> ScreenShareSize.compute(minimum, usableScreen, SCREEN_SHARE, SCREEN_SHARE));
  }
}
