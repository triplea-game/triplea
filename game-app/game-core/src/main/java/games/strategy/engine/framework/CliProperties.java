package games.strategy.engine.framework;

import org.jetbrains.annotations.NonNls;

/** A collection of all CLI related constants. */
public class CliProperties {
  @NonNls public static final String TRIPLEA_GAME = "triplea.game";
  @NonNls public static final String TRIPLEA_SERVER = "triplea.server";
  @NonNls public static final String TRIPLEA_CLIENT = "triplea.client";
  @NonNls public static final String TRIPLEA_HOST = "triplea.host";
  @NonNls public static final String TRIPLEA_PORT = "triplea.port";
  @NonNls public static final String TRIPLEA_NAME = "triplea.name";
  @NonNls public static final String SERVER_PASSWORD = "triplea.server.password";
  @NonNls public static final String LOBBY_URI = "triplea.lobby.uri";
  @NonNls public static final String LOBBY_GAME_COMMENTS = "triplea.lobby.game.comments";
  @NonNls public static final String MAP_FOLDER = "triplea.map.folder";
  @NonNls public static final String TRIPLEA_MAP_DOWNLOAD = "triplea.map.download";

  private CliProperties() {}
}
