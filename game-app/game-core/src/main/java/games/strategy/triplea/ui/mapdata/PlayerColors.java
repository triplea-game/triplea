package games.strategy.triplea.ui.mapdata;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.ColorUtils;

@RequiredArgsConstructor
public final class PlayerColors {
  @NonNls public static final String PROPERTY_COLOR_PREFIX = "color.";
  @NonNls public static final String PLAYER_NAME_IMPASSABLE = "Impassable";
  @NonNls public static final String PLAYER_NAME_IMPASSABLE_LEGACY_SPELLING = "Impassible";
  public static final Color DEFAULT_IMPASSABLE_COLOR = ColorUtils.fromHexString("DEB887");

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

  @VisibleForTesting static final int DEFAULT_COLOR_COUNT = COLORS.size();

  private final Iterator<Color> colorIterator = COLORS.iterator();
  private final Map<String, Color> playerColors = new HashMap<>();
  private final Properties mapProperties;

  /**
   * Returns the color that should be used for a given player. If the player's color is not defined
   * in map.properties then a default color is randomly generated.
   */
  Color getPlayerColor(final String playerName) {
    Preconditions.checkArgument(
        !playerName.equals(PLAYER_NAME_IMPASSABLE)
            && !playerName.equals(PLAYER_NAME_IMPASSABLE_LEGACY_SPELLING),
        "Illegal player name: %s, use the method 'getImpassableColor()' instead",
        playerName);

    // NOTE: we do *not* use computeIfAbsent here to avoid
    // 'java.util.ConcurrentModificationException'
    Color playerColor = playerColors.get(playerName);
    if (playerColor == null) {
      playerColor = computePlayerColor(playerName);
      playerColors.put(playerName, playerColor);
    }
    return playerColor;
  }

  private Color computePlayerColor(final String playerName) {
    return Optional.ofNullable(mapProperties.getProperty(PROPERTY_COLOR_PREFIX + playerName))
        .map(ColorUtils::fromHexString)
        .orElseGet(() -> nextColor(playerName));
  }

  /**
   * Returns the next available default color or generates a random one if no more default colors
   * are available.
   */
  private Color nextColor(final String playerName) {
    return colorIterator.hasNext()
        ? colorIterator.next()
        : ColorUtils.randomColor(playerName.hashCode());
  }

  /**
   * Returns the color for the 'Impassable player', this is to mean impassible territories. If the
   * impassable player color is not defined in map.properties then a default is returned.
   */
  Color getImpassableColor() {
    return Optional.ofNullable(
            mapProperties.getProperty(PROPERTY_COLOR_PREFIX + PLAYER_NAME_IMPASSABLE))
        .or(
            () ->
                Optional.ofNullable(
                    mapProperties.getProperty(
                        PROPERTY_COLOR_PREFIX + PLAYER_NAME_IMPASSABLE_LEGACY_SPELLING)))
        .map(ColorUtils::fromHexString)
        .orElse(DEFAULT_IMPASSABLE_COLOR);
  }
}
