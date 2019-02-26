package org.triplea.lobby.common;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;

/**
 * A service that provides user management operations. Currently, all operations may only target the calling user (i.e.
 * {@code userName} must match the name of the calling user).
 */
public interface IUserManager extends IRemote {
  RemoteName REMOTE_NAME = new RemoteName("games.strategy.engine.lobby.server.USER_MANAGER", IUserManager.class);

  /**
   * Update the user info, returning an error string if an error occurs.
   */
  String updateUser(String userName, String emailAddress, String hashedPassword);

  DBUser getUserInfo(String userName);
}
