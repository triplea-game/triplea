package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to create/read/delete bad words (there is no update).
 */
public final class BadWordController implements BadWordDao {
  @Override
  public void addBadWord(final String word) {
    try (Connection con = Database.getPostgresConnection();
        // If the word already is present we don't need to add it twice.
        PreparedStatement ps = con.prepareStatement("insert into bad_words (word) values (?) on conflict do nothing")) {
      ps.setString(1, word);
      ps.execute();
      con.commit();
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error inserting banned word:" + word, sqle);
    }
  }

  @Override
  public List<String> list() {
    final String sql = "select word from bad_words";

    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      final List<String> badWords = new ArrayList<>();
      while (rs.next()) {
        badWords.add(rs.getString(1));
      }
      return badWords;
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error reading bad words", sqle);
    }
  }
}
