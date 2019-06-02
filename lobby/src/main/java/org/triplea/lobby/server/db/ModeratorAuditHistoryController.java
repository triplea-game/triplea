package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;

@AllArgsConstructor
final class ModeratorAuditHistoryController implements ModeratorAuditHistoryDao {

  private final Supplier<Connection> connection;

  @Override
  public void addAuditRecord(final ModeratorAuditHistoryDao.AuditArgs auditArgs) {
    final int moderatorId = lookupModeratorId(auditArgs);
    insertAuditRecord(moderatorId, auditArgs);
  }

  // TODO: make method private, as a private method PMD thinks this method is unused.
  int lookupModeratorId(final AuditArgs auditArgs) {
    final String sql = "select id from lobby_user where username = ?";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, auditArgs.getModeratorName());
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new IllegalStateException("Failed to find a moderator by name: " + auditArgs.getModeratorName());
        }
        return rs.getInt("id");
      }
    } catch (final SQLException e) {
      throw new DatabaseException("DB error looking up moderator name", e);
    }
  }

  // TODO: make method private, as a private method PMD thinks this method is unused.
  void insertAuditRecord(final int moderatorId, final AuditArgs auditArgs) {
    final String sql = ""
        + "insert into moderator_action_history "
        + "  (lobby_user_id, action_name, action_target) "
        + "values (?, ?, ?)";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, moderatorId);
      ps.setString(2, auditArgs.getActionName().name());
      ps.setString(3, auditArgs.getActionTarget());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException("Error inserting audit record: " + auditArgs, e);
    }
  }
}
