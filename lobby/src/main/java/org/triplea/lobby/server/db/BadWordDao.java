package org.triplea.lobby.server.db;

/**
 * Data access object for the bad word table.
 */
public interface BadWordDao {
  /**
   * Adds the specified bad word to the table.
   *
   * @param word The bad word to add.
   */
  void addBadWord(String word);

  /**
   * Checks if a given string contains a bad word.
   *
   * @param testString The value to check for a bad word
   *
   * @return Returns true if the parameter contains a bad word (case insensitive).
   */
  boolean containsBadWord(String testString);
}
