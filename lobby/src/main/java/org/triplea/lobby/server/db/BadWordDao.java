package org.triplea.lobby.server.db;

/** Data access object for the bad word table. */
public interface BadWordDao {
  /**
   * Checks if a given string contains a bad word.
   *
   * @param testString The value to check for a bad word
   * @return Returns true if the parameter contains a bad word (case insensitive).
   */
  boolean containsBadWord(String testString);
}
