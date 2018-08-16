package games.strategy.engine.lobby.common;

import games.strategy.engine.message.RemoteName;
import games.strategy.util.Version;

/**
 * A collection of constants used by both the lobby client and server.
 */
public final class LobbyConstants {
  public static final String ADMIN_USERNAME = "Admin";
  public static final String LOBBY_CHAT = "_LOBBY_CHAT";
  public static final Version LOBBY_VERSION = new Version(1, 0, 0);
  public static final String LOBBY_WATCHER_NAME = "lobby_watcher";
  public static final RemoteName MODERATOR_CONTROLLER_REMOTE_NAME =
      new RemoteName("games.strategy.engine.lobby.server.ModeratorController:Global", IModeratorController.class);

  private LobbyConstants() {}
}
