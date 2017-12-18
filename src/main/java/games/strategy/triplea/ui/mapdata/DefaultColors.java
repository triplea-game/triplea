package games.strategy.triplea.ui.mapdata;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class DefaultColors {
  private final List<Color> colors = new ArrayList<>(Arrays.asList(
      Color.RED,
      Color.MAGENTA,
      Color.YELLOW,
      Color.ORANGE,
      Color.CYAN,
      Color.GREEN,
      Color.PINK,
      Color.GRAY));

  /**
   * @throws IndexOutOfBoundsException If the available default colors have been exhausted.
   */
  Color nextColor() {
    return colors.remove(0);
  }
}
