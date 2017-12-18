package games.strategy.triplea.ui.mapdata;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.ImmutableList;

final class DefaultColors {
  private static final List<Color> COLORS = ImmutableList.of(
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
