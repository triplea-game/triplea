package games.strategy.triplea.ui.mapdata;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.util.Iterator;
import java.util.NoSuchElementException;

final class PlayerColors {
  @VisibleForTesting
  static final ImmutableList<Color> COLORS =
      ImmutableList.of(
          Color.RED,
          Color.MAGENTA,
          Color.YELLOW,
          Color.ORANGE,
          Color.CYAN,
          Color.GREEN,
          Color.PINK,
          Color.GRAY);

  private final Iterator<Color> colorIterator = COLORS.iterator();

  /**
   * Returns the next available default color.
   *
   * @throws NoSuchElementException If the available default colors have been exhausted.
   */
  Color nextColor() {
    return colorIterator.next();
  }
}
