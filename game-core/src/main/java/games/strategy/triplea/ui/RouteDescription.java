package games.strategy.triplea.ui;

import games.strategy.engine.data.Route;
import java.awt.Image;
import java.awt.Point;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class RouteDescription {
  private final @Nullable Route route;
  // this point is in map co-ordinates, un scaled
  private final @Nullable Point start;
  // this point is in map co-ordinates, un scaled
  private final @Nullable Point end;
  private final @Nullable Image cursorImage;

  @Override
  public int hashCode() {
    return Objects.hash(route, cursorImage);
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RouteDescription)) {
      return false;
    }

    final RouteDescription other = (RouteDescription) o;
    if (!Objects.equals(start, other.start)) {
      return false;
    }
    if (!Objects.equals(route, other.route)) {
      return false;
    }
    if ((end == null && other.end != null) || (other.end == null && end != null)) {
      return false;
    }
    if (cursorImage != other.cursorImage) {
      return false;
    }
    // we dont want to be updating for every small change,
    // if the end points are close enough, they are close enough
    if (other.end == null) {
      return true;
    }
    int diffX = end.x - other.end.x;
    diffX *= diffX;
    int diffY = end.y - other.end.y;
    diffY *= diffY;
    final int endDiff = (int) Math.sqrt((double) diffX + diffY);
    return endDiff < 6;
  }
}
