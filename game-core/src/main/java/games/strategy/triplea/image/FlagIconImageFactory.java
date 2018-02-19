package games.strategy.triplea.image;

import java.awt.Image;

import games.strategy.engine.data.PlayerID;

public class FlagIconImageFactory extends ImageFactory {
  private static final String PREFIX = "flags/";

  /** Creates new FlagIconImageFactory. */
  public FlagIconImageFactory() {}

  public Image getFlag(final PlayerID id) {
    final String key = PREFIX + id.getName() + ".gif";
    final String key2 = PREFIX + id.getName() + ".png";
    return getImage(key, key2, true);
  }

  public Image getSmallFlag(final PlayerID id) {
    final String key = PREFIX + id.getName() + "_small.gif";
    final String key2 = PREFIX + id.getName() + "_small.png";
    return getImage(key, key2, true);
  }

  public Image getLargeFlag(final PlayerID id) {
    final String key = PREFIX + id.getName() + "_large.png";
    return getImage(key, true);
  }

  public Image getFadedFlag(final PlayerID id) {
    final String key = PREFIX + id.getName() + "_fade.gif";
    final String key2 = PREFIX + id.getName() + "_fade.png";
    return getImage(key, key2, true);
  }

  public Image getConvoyFlag(final PlayerID id) {
    final String key = PREFIX + id.getName() + "_convoy.gif";
    final String key2 = PREFIX + id.getName() + "_convoy.png";
    return getImage(key, key2, true);
  }
}
