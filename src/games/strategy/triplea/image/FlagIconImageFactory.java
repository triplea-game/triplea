package games.strategy.triplea.image;

import java.awt.Image;

import games.strategy.engine.data.PlayerID;

public class FlagIconImageFactory extends ImageFactory {
  public static final int FLAG_ICON_WIDTH = 30;
  public static final int FLAG_ICON_HEIGHT = 15;
  public static final int SMALL_FLAG_ICON_WIDTH = 12;
  public static final int SMALL_FLAG_ICON_HEIGHT = 7;
  private final String PREFIX = "flags/";

  /** Creates new IconImageFactory */
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
