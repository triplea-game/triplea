package games.strategy.triplea.image;

import games.strategy.engine.data.PlayerId;
import java.awt.Image;

/** A factory for creating various player (nation, power, etc.) flag images. */
public class FlagIconImageFactory extends ImageFactory {
  private static final String PREFIX = "flags/";

  public FlagIconImageFactory() {}

  public Image getFlag(final PlayerId id) {
    final String key = PREFIX + id.getName() + ".gif";
    final String key2 = PREFIX + id.getName() + ".png";
    return getImage(key, key2, true);
  }

  public Image getSmallFlag(final PlayerId id) {
    final String key = PREFIX + id.getName() + "_small.gif";
    final String key2 = PREFIX + id.getName() + "_small.png";
    return getImage(key, key2, true);
  }

  public Image getLargeFlag(final PlayerId id) {
    final String key = PREFIX + id.getName() + "_large.png";
    return getImage(key, true);
  }

  public Image getFadedFlag(final PlayerId id) {
    final String key = PREFIX + id.getName() + "_fade.gif";
    final String key2 = PREFIX + id.getName() + "_fade.png";
    return getImage(key, key2, true);
  }

  public Image getConvoyFlag(final PlayerId id) {
    final String key = PREFIX + id.getName() + "_convoy.gif";
    final String key2 = PREFIX + id.getName() + "_convoy.png";
    return getImage(key, key2, true);
  }
}
