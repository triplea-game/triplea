package games.strategy.engine.framework;

import org.jetbrains.annotations.NonNls;

/** A collection of all CLI related constants. */
public class CliProperties {
  @NonNls public static final String TRIPLEA_GAME = "triplea.game";
  @NonNls public static final String TRIPLEA_SERVER = "triplea.server"; // true/false
  @NonNls public static final String TRIPLEA_CLIENT = "triplea.client"; // true/false
  @NonNls public static final String TRIPLEA_HOST = "triplea.host";
  @NonNls public static final String TRIPLEA_PORT = "triplea.port";
  @NonNls public static final String TRIPLEA_NAME = "triplea.name";
  @NonNls public static final String TRIPLEA_START = "triplea.start";
  @NonNls public static final String SERVER_PASSWORD = "triplea.server.password";
  @NonNls public static final String LOBBY_URI = "triplea.lobby.uri";
  @NonNls public static final String LOBBY_GAME_COMMENTS = "triplea.lobby.game.comments";
  @NonNls public static final String TRIPLEA_MAP_DOWNLOAD = "triplea.map.download";
  @NonNls public static final String TRIPLEA_MAP_DOWNLOAD_PREFIX = "triplea:";

  @NonNls public static final String TRIPLEA_START_LOCAL = "local";
  @NonNls public static final String TRIPLEA_START_PBF = "pbf";
  @NonNls public static final String TRIPLEA_START_PBEM = "pbem";
  @NonNls public static final String TRIPLEA_START_LOBBY = "lobby";

  private CliProperties() {}
}
