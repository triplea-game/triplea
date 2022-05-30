package games.strategy.engine.framework;

/**
 * GameRunner - The entrance class with the main method. In this class commonly used constants are
 * getting defined and the Game is being launched
 */
public final class GameRunner {
  public static final String TRIPLEA_HEADLESS = "triplea.headless";
  public static final String BOT_GAME_HOST_COMMENT = "automated_host";
  public static final String BOT_GAME_HOST_NAME_PREFIX = "Bot";
  public static final int PORT = 3300;
  public static final int MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME = 20;

  public static boolean headless() {
    return Boolean.parseBoolean(System.getProperty(TRIPLEA_HEADLESS, "false"));
  }
}
