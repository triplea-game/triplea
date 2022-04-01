package games.strategy.triplea.image;

import games.strategy.triplea.image.UnitImageFactory.ImageKey;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.UnitCategory;

public class MissingImageException extends RuntimeException {
  private static final long serialVersionUID = -1278382391054838356L;

  public MissingImageException(final ImageKey imageKey) {
    super(
        "Missing image: "
            + imageKey
            + ", search folders: "
            + UiContext.getResourceLoader().getAssetPaths());
  }
}
