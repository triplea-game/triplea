package games.strategy.engine.lobby.server.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.TestUserUtils;
import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.login.UserType;

public final class AccessLogControllerIntegrationTest {
  private final AccessLogController accessLogController = new AccessLogController();

  @Test
  public void insert_ShouldInsertNewRecord() throws Exception {
    final Instant instant = Instant.now();
    final User user = TestUserUtils.newUser();

    for (final UserType userType : UserType.values()) {
      accessLogController.insert(instant, user, userType);

      thenAccessLogRecordShouldExist(instant, user, userType);
    }
  }

  private static void thenAccessLogRecordShouldExist(final Instant instant, final User user, final UserType userType)
      throws Exception {
    final String sql = ""
        + "select count(*) from access_log "
        + "where access_time=? and username=? and ip=?::inet and mac=? and registered=?";
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setTimestamp(1, Timestamp.from(instant));
      ps.setString(2, user.getUsername());
      ps.setString(3, user.getInetAddress().getHostAddress());
      ps.setString(4, user.getHashedMacAddress());
      ps.setBoolean(5, userType == UserType.REGISTERED);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertThat(rs.getInt(1), is(1));
        } else {
          fail("access log record does not exist");
        }
      }
    }
  }
}
