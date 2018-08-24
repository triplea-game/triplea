package games.strategy.engine.lobby.server.db;

import java.util.List;

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
   * Returns a collection of all bad words in the table.
   *
   * @return A collection of all bad words in the table.
   */
  List<String> list();
}
