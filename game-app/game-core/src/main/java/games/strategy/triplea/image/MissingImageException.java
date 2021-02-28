package games.strategy.triplea.image;

import games.strategy.triplea.image.UnitImageFactory.ImageKey;
import games.strategy.triplea.util.UnitCategory;

public class MissingImageException extends RuntimeException {
  private static final long serialVersionUID = -1278382391054838356L;

  public MissingImageException(final UnitCategory unitCategory) {
    super("Missing image for: " + unitCategory.getOwner() + ":" + unitCategory.getType());
  }

  public MissingImageException(final ImageKey imageKey) {
    super("Missing image: " + imageKey);
  }
}
