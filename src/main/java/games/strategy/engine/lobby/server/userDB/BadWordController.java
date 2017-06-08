package games.strategy.engine.lobby.server.userDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitiy class to create/read/delete bad words (there is no update).
 */
public class BadWordController {
  private static final Logger s_logger = Logger.getLogger(BadWordController.class.getName());

  public void addBadWord(final String word) {
    s_logger.fine("Adding bad word word:" + word);
    final Connection con = Database.getDerbyConnection();
    try {
      final PreparedStatement ps = con.prepareStatement("insert into bad_words (word) values (?)");
      ps.setString(1, word);
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      if (sqle.getErrorCode() == 30000) {
        // this is ok
        // the word is bad as expected
        s_logger.info("Tried to create duplicate banned word:" + word + " error:" + sqle.getMessage());
        return;
      }
      s_logger.log(Level.SEVERE, "Error inserting banned word:" + word, sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  void removeBannedWord(final String word) {
    s_logger.fine("Removing banned word:" + word);
    final Connection con = Database.getDerbyConnection();
    try {
      final PreparedStatement ps = con.prepareStatement("delete from bad_words where word = ?");
      ps.setString(1, word);
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      s_logger.log(Level.SEVERE, "Error deleting banned word:" + word, sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  public List<String> list() {
    final String sql = "select word from bad_words";
    final Connection con = Database.getDerbyConnection();
    try {
      final PreparedStatement ps = con.prepareStatement(sql);
      final ResultSet rs = ps.executeQuery();
      final List<String> rVal = new ArrayList<>();
      while (rs.next()) {
        rVal.add(rs.getString(1));
      }
      rs.close();
      ps.close();
      return rVal;
    } catch (final SQLException sqle) {
      s_logger.info("Error reading bad words error:" + sqle.getMessage());
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }
}
