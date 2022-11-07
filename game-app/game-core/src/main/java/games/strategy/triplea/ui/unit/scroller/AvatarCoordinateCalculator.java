package games.strategy.triplea.ui.unit.scroller;

import com.google.common.base.Preconditions;
import java.awt.Point;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.ToString;

/**
 * Calculates where to draw unit avatar images.The unit avatar images are for all images in a
 * territory, a stack of units, we draw these images with an offset from one another, this
 * calculator computes those offset values.
 */
@Builder
@ToString
public class AvatarCoordinateCalculator {
  @Nonnull private final Integer renderingWidth;
  @Nonnull private final Integer renderingHeight;
  @Nonnull private final Integer unitImageCount;
  @Nonnull private final Integer unitImageWidth;
  @Nonnull private final Integer unitImageHeight;

  /** Returns draw coordinates for each unit image in proper rendering order. */
  List<Point> computeDrawCoordinates() {
    Preconditions.checkArgument(
        unitImageWidth > 1, "Unit image width must be at least 2px, is: " + unitImageWidth);
    if (unitImageCount == 0) {
      return List.of();
    }

    final int horizontalSpacing = renderingWidth / (unitImageCount + 1);
    final int y = renderingHeight - unitImageHeight;
    final int halfImageWidth = unitImageWidth / 2;

    return IntStream.range(0, unitImageCount)
        .map(i -> (i + 1) * horizontalSpacing - halfImageWidth)
        .mapToObj(x -> new Point(x, y))
        .collect(Collectors.toList());
  }
}
