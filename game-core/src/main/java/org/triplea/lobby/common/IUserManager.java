package org.triplea.lobby.common;

import games.strategy.engine.lobby.PlayerName;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;

/**
 * A service that provides user management operations. Currently, all operations may only target the
 * calling user (i.e. {@code username} must match the name of the calling user).
 */
public interface IUserManager extends IRemote {
  RemoteName REMOTE_NAME =
      new RemoteName("games.strategy.engine.lobby.server.USER_MANAGER", IUserManager.class);

  /** Update the user info, returning an error string if an error occurs. */
  String updateUser(PlayerName username, String emailAddress, String hashedPassword);

  String getUserEmail(PlayerName username);
}
