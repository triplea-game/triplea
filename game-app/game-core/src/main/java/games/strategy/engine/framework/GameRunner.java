package games.strategy.engine.framework;

import org.jetbrains.annotations.NonNls;

/** In this class commonly used constants are getting defined. */
public final class GameRunner {
  @NonNls public static final String TRIPLEA_HEADLESS = "triplea.headless";
  @NonNls public static final String BOT_GAME_HOST_COMMENT = "automated_host";
  @NonNls public static final String BOT_GAME_HOST_NAME_PREFIX = "Bot";
  public static final int PORT = 3300;

  public static boolean headless() {
    return Boolean.parseBoolean(System.getProperty(TRIPLEA_HEADLESS, "false"));
  }

  public static boolean exitOnEndGame() {
    return Boolean.parseBoolean(System.getProperty("triplea.exit.on.game.end", "false"));
  }
}
