package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

/** Utility class to create/read/delete banned usernames (there is no update). */
@AllArgsConstructor
class UsernameBlacklistController implements UsernameBlacklistDao {
  private final Supplier<Connection> connection;
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;
  private final UserJdbiDao userJdbiDao;

  /** This implementation has the side effect of removing any usernames whose ban has expired. */
  @Override
  public boolean isUsernameBanned(final String username) {
    final String sql = "select 1 from banned_username where lower(username) = lower(?)";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error for testing banned username existence: " + username, e);
    }
  }
}
