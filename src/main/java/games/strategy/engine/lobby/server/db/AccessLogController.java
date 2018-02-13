package games.strategy.engine.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.login.UserType;

/**
 * Implementation of {@link AccessLogDao} for a Postgres database.
 */
public final class AccessLogController implements AccessLogDao {
  @Override
  public void insert(final Instant instant, final User user, final UserType userType) throws SQLException {
    checkNotNull(instant);
    checkNotNull(user);
    checkNotNull(userType);

    final String sql = ""
        + "insert into access_log "
        + "  (access_time, username, ip, mac, registered) "
        + "  values "
        + "  (?, ?, ?::inet, ?, ?)";
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.from(instant));
      ps.setString(2, user.getUsername());
      ps.setString(3, user.getInetAddress().getHostAddress());
      ps.setString(4, user.getHashedMacAddress());
      ps.setBoolean(5, userType == UserType.REGISTERED);
      ps.execute();
      conn.commit();
    }
  }
}
