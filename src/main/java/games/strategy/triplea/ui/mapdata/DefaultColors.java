package games.strategy.triplea.ui.mapdata;

import java.awt.Color;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

final class DefaultColors {
  @VisibleForTesting
  static final ImmutableList<Color> COLORS = ImmutableList.of(
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
   * @throws NoSuchElementException If the available default colors have been exhausted.
   */
  Color nextColor() {
    return colorIterator.next();
  }
}
