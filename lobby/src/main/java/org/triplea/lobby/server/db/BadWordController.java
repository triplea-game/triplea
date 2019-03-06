package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
  public List<String> list() {
    final String sql = "select word from bad_words";

    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      final List<String> badWords = new ArrayList<>();
      while (rs.next()) {
        badWords.add(rs.getString(1));
      }
      return badWords;
    } catch (final SQLException e) {
      throw new DatabaseException("Error reading bad words", e);
    }
  }
}
