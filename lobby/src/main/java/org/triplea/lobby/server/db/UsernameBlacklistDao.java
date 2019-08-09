package org.triplea.lobby.server.db;

/** Data access object for the banned username table. */
public interface UsernameBlacklistDao {
  /**
   * Adds the specified banned username to the table if it does not exist or updates the instant at
   * which the ban will expire if it already exists.
   *
   * @param usernameToBan The username will be banned.
   * @param moderatorName The name of the moderator executing the ban.
   * @throws IllegalStateException If an error occurs while adding, updating, or removing the ban.
   */
  void addName(String usernameToBan, String moderatorName);

  /**
   * Indicates if the specified username is banned.
   *
   * @param username The username to query for a ban.
   * @return True if the username is banned, false otherwise.
   */
  boolean isUsernameBanned(String username);
}
