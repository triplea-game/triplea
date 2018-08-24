package games.strategy.engine.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.login.UserType;

/**
 * Implementation of {@link AccessLogDao} for a Postgres database.
 */
public final class AccessLogController extends AbstractController implements AccessLogDao {
  public AccessLogController(final Database database) {
    super(database);
  }

  @Override
  public void insert(final User user, final UserType userType) throws SQLException {
    checkNotNull(user);
    checkNotNull(userType);

    final String sql = "insert into access_log (username, ip, mac, registered) values (?, ?::inet, ?, ?)";
    try (Connection conn = newDatabaseConnection();
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
