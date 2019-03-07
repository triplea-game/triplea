package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;

/**
 * Utility class to create/read/delete bad words (there is no update).
 */
@AllArgsConstructor
final class BadWordController implements BadWordDao {
  private final Supplier<Connection> connection;

  @Override
  public void addBadWord(final String word) {
    try (Connection con = connection.get();
        // If the word already is present we don't need to add it twice.
        PreparedStatement ps = con.prepareStatement("insert into bad_words (word) values (?) on conflict do nothing")) {
      ps.setString(1, word);
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException("Error inserting banned word: " + word, e);
    }
  }

  @Override
  public boolean containsBadWord(final String testString) {
    if (testString.isEmpty()) {
      return false;
    }

    // Query to count if at least one bad word value is contained in the testString.
    final String sql =
        "select count(bw.word) "
            + "from bad_words bw "
            + "where ? like '%' || lower(bw.word) || '%' "
            + "limit 1";

    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {

      ps.setString(1, testString.toLowerCase());

      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getInt(1) > 0;
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error reading bad words", e);
    }
  }
}
