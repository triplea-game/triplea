package org.triplea.lobby.server.db;

/** Data access object for the banned username table. */
public interface UsernameBlacklistDao {
  /**
   * Indicates if the specified username is banned.
   *
   * @param username The username to query for a ban.
   * @return True if the username is banned, false otherwise.
   */
  boolean isUsernameBanned(String username);
}
