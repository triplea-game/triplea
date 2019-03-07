package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;

import org.triplea.lobby.server.User;
import org.triplea.lobby.server.login.UserType;

import lombok.AllArgsConstructor;

/**
 * Implementation of {@link AccessLogDao} for a Postgres database.
 */
@AllArgsConstructor
final class AccessLogController implements AccessLogDao {
  private final Supplier<Connection> connection;

  @Override
  public void insert(final User user, final UserType userType) throws SQLException {
    checkNotNull(user);
    checkNotNull(userType);

    final String sql = "insert into access_log (username, ip, mac, registered) values (?, ?::inet, ?, ?)";
    try (Connection conn = connection.get();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.getUsername());
      ps.setString(2, user.getInetAddress().getHostAddress());
      ps.setString(3, user.getHashedMacAddress());
      ps.setBoolean(4, userType == UserType.REGISTERED);
      ps.execute();
      conn.commit();
    }
  }
}
