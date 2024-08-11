package games.strategy.triplea.image;

import games.strategy.engine.data.GamePlayer;
import java.awt.Image;

/** A factory for creating various player (nation, power, etc.) flag images. */
public class FlagIconImageFactory extends ImageFactory {
  private static final String PREFIX = "flags/";

  public Image getFlag(final GamePlayer gamePlayer) {
    @NonNls final String key = PREFIX + gamePlayer.getName() + ".gif";
    @NonNls final String key2 = PREFIX + gamePlayer.getName() + ".png";
    return getImageOrThrow(key, key2);
  }

  public Image getSmallFlag(final GamePlayer gamePlayer) {
    @NonNls final String key = PREFIX + gamePlayer.getName() + "_small.gif";
    @NonNls final String key2 = PREFIX + gamePlayer.getName() + "_small.png";
    return getImageOrThrow(key, key2);
  }

  public Image getLargeFlag(final GamePlayer gamePlayer) {
    @NonNls final String key = PREFIX + gamePlayer.getName() + "_large.png";
    return getImageOrThrow(key);
  }

  public Image getFadedFlag(final GamePlayer gamePlayer) {
    @NonNls final String key = PREFIX + gamePlayer.getName() + "_fade.gif";
    @NonNls final String key2 = PREFIX + gamePlayer.getName() + "_fade.png";
    return getImageOrThrow(key, key2);
  }

  public Image getConvoyFlag(final GamePlayer gamePlayer) {
    @NonNls final String key = PREFIX + gamePlayer.getName() + "_convoy.gif";
    @NonNls final String key2 = PREFIX + gamePlayer.getName() + "_convoy.png";
    return getImageOrThrow(key, key2);
  }
}
