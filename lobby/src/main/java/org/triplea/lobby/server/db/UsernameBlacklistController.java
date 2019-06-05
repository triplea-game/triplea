package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;

/**
 * Utility class to create/read/delete banned usernames (there is no update).
 */
@AllArgsConstructor
class UsernameBlacklistController implements UsernameBlacklistDao {
  private final Supplier<Connection> connection;
  private final ModeratorAuditHistoryDao moderatorAuditHistoryDao;

  @Override
  public void addName(final String usernameToBan, final String moderatorName) {
    checkNotNull(usernameToBan);
    checkNotNull(moderatorName);

    final String sql = ""
        + "insert into banned_usernames "
        + "  (username, mod_username) values (?, ?) "
        + "on conflict (username) do nothing";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, usernameToBan);
      ps.setString(2, moderatorName);
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException("Error inserting banned username: " + usernameToBan, e);
    }

    moderatorAuditHistoryDao.addAuditRecord(
        ModeratorAuditHistoryDao.AuditArgs.builder()
            .moderatorName(moderatorName)
            .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_USERNAME)
            .actionTarget(usernameToBan)
            .build());
  }

  /**
   * This implementation has the side effect of removing any usernames whose ban has expired.
   */
  @Override
  public boolean isUsernameBanned(final String username) {
    final String sql = "select 1 from banned_usernames where lower(username) = lower(?)";
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
