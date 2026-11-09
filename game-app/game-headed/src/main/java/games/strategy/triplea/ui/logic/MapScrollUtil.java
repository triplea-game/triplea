package games.strategy.triplea.ui.logic;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Helper class to deal with infinitely scrolling maps. */
public final class MapScrollUtil {

  private MapScrollUtil() {}

  /**
   * Creates an unmodifiable List of possible translations on the given map. If a map has "fixed
   * borders" i.e. you can't infinitely scroll along at least one axis the returned list will just
   * contain a single identity {@linkplain AffineTransform}.
   *
   * <p>If the map however is infinitely scrolling along an axis, the amount of {@linkplain
   * AffineTransform}s will multiply by 3. Each {@linkplain AffineTransform} is a translation by a
   * multiple (between -1 and 1) of mapHeight/mapWidth.
   *
   * @param isInfiniteX True if the map wraps left and right
   * @param isInfiniteY True if the map wraps top and bottom
   * @param mapWidth Unscaled width of the map
   * @param mapHeight Unscaled height of the map
   * @return An unmodifiable List containing 9-1 {@linkplain AffineTransform}s
   */
  public static List<AffineTransform> getPossibleTranslations(
      final boolean isInfiniteX,
      final boolean isInfiniteY,
      final int mapWidth,
      final int mapHeight) {
    final List<AffineTransform> result = new ArrayList<>(3); // 3 is probably the most common value
    result.add(new AffineTransform());
    if (isInfiniteX && isInfiniteY) {
      result.addAll(
          List.of(
              AffineTransform.getTranslateInstance(-mapWidth, -mapHeight),
              AffineTransform.getTranslateInstance(-mapWidth, +mapHeight),
              AffineTransform.getTranslateInstance(+mapWidth, -mapHeight),
              AffineTransform.getTranslateInstance(+mapWidth, +mapHeight)));
    }
    if (isInfiniteX) {
      result.addAll(
          List.of(
              AffineTransform.getTranslateInstance(-mapWidth, 0),
              AffineTransform.getTranslateInstance(+mapWidth, 0)));
    }
    if (isInfiniteY) {
      result.addAll(
          List.of(
              AffineTransform.getTranslateInstance(0, -mapHeight),
              AffineTransform.getTranslateInstance(0, +mapHeight)));
    }
    return Collections.unmodifiableList(result);
  }
}
