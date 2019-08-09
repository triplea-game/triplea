package org.triplea.lobby.server.db;

import org.triplea.lobby.server.User;
import org.triplea.lobby.server.login.UserType;

/** Data access object for the access log table. */
public interface AccessLogDao {
  /**
   * Inserts a new record in the access log table.
   *
   * @param user The user who accessed the lobby.
   * @param userType The type of the user who accessed the lobby.
   */
  void insert(User user, UserType userType);
}
